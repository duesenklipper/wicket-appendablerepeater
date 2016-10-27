package de.wicketbuch.extensions.appendablerepeater.gridview;

import static de.wicketbuch.extensions.appendablerepeater.listview.AppendableListView.SCRIPT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import de.wicketbuch.extensions.appendablerepeater.listview.AppendableListView;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * A {@link GridView} implementation that can dynamically append items via AJAX,
 * without repainting the entire grid. It
 * is very nearly a drop-in replacement for GridView, only the {@link Item}
 * types have been narrowed to {@link AppendableItem} and
 * {@link AppendableRowItem}.
 * <p>
 * {@code AppendableGridView} works differently from
 * {@link AppendableListView} in that it does not have an append method that
 * adds elements to the model. Instead, you append to your model manually and
 * then call {@link #itemsAppended(AjaxRequestTarget)}. Newly added items
 * will be added to the existing elements in the browser. If the current page
 * is "full" and new items would be added to a different page, no new
 * elements will be inserted on this page - instead, the {@code
 * AppendableGridView} will jump to the first page with new elements.
 * <p>
 * To facilitate animations and other things, this class offers the following
 * callbacks:
 * <ul>
 * <li>{@link #onAppendItem(AppendableItem, AjaxRequestTarget)} is
 * called when a single item is appended, replacing an empty slot in an
 * already existing row.
 * </li>
 * <li>{@link #onAppendRow(AppendableRowItem, AjaxRequestTarget)} is
 * called when a new row is appended, containing at least one "filled"
 * slot. onAppendItem will <em>not</em> be called for the elements in
 * this row.</li>
 * <li>
 * {@link #onPageChangeAfterAppend(AjaxRequestTarget)} is called when
 * no new elements are added on the current page but instead the page
 * is changed to where new elements have appeared.
 * </li>
 * </ul>
 *
 * @param <T> The list element type
 * @author Carl-Eric Menzel cmenzel@wicketbuch.de
 */
public abstract class AppendableGridView<T> extends GridView<T>
{
	/**
	 * Contains the empty items that were used to fill up non-complete rows
	 * during the last render. These will be used to display new items in
	 * {@link #itemsAppended(AjaxRequestTarget)}. The empty items are never
	 * stale because they are recreated on each rendering.
	 */
	private final SortedMap<Integer, AppendableItem> renderedEmptyItems = new
			TreeMap<>();

	/**
	 * RowItems that are added (via {@link #add(Component...)}) are
	 * registered here so they can be submitted to
	 * {@link #onAppendRow(AppendableRowItem, AjaxRequestTarget)}. This list
	 * is discarded after its contents were used.
	 */
	private List<AppendableRowItem> newlyAddedRows;

	private List<AppendableItem> appendedItems;

	/**
	 * The number of items in the DataProvider after the last render. This is
	 * used to determine the number of new items in
	 * {@link #itemsAppended(AjaxRequestTarget)}.
	 */
	private long lastItemCount = 0;

	/**
	 * The tag used for the row items. This is lazily determined and then
	 * cached.
	 */
	private String rowTagName;

	/**
	 * The markup id of the last row that was rendered. This used to figure
	 * out the insertion point for ajax updates.
	 */
	private String lastRenderedRowMarkupId;

	/**
	 * This indicates whether we are currently in the process of appending
	 * rows. If we are, then newly added rows are passed to
	 * {@link #onAppendRow(AppendableRowItem, AjaxRequestTarget)} to let them
	 * be animated. If not, we are in a normal page render or e.g. in a page
	 * change (via {@link PagingNavigator}, for example) and newly added rows
	 * are simply regenerated rows due to the item reuse strategy. They do
	 * not need the animation callback then.
	 */
	private boolean appending = false;

	public AppendableGridView(String id, IDataProvider<T> dataProvider)
	{
		super(id, dataProvider);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		// we will need the parent to re-render in case we are changing the
		// current page or are appending to a previously empty grid, so we
		// tell it to have a markup id.
		getParent().setOutputMarkupId(true);
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		// empty items from the laster render will be discarded anyway now, so
		// we don't need to track them anymore
		renderedEmptyItems.clear();

		// we are interested in newly added rows only if we are currently
		// appending. if we are not appending, rows were added in the normal
		// process of rebuilding the repeater - so we do not need the
		// appending animation.
		if (appending && newlyAddedRows != null)
		{
			AjaxRequestTarget ajax =
					RequestCycle.get().find(AjaxRequestTarget.class);
			if (ajax != null)
			{
				// only animate if we are actually in an ajax request
				for (AppendableRowItem newlyAddedRow : newlyAddedRows)
				{
					onAppendRow(newlyAddedRow, ajax);
				}
			}
		}
		// we are done with appending now
		appending = false;
	}

	@Override
	protected AppendableItem newEmptyItem(String id, int index)
	{
		return new AppendableItem(id, index);
	}

	@Override
	protected AppendableItem newItem(String id, int index, IModel<T> model)
	{
		return new AppendableItem(id, index, model);
	}

	@Override
	protected AppendableRowItem newRowItem(String id, int index)
	{
		return new AppendableRowItem(id, index);
	}

	/**
	 * Call this method after adding items to the {@code DataProvider} given to
	 * this {@code AppendableGridView}. It will then appropriately insert the
	 * new elements via ajax.
	 * @param ajax the currently active {@link AjaxRequestTarget}
	 */
	public void itemsAppended(AjaxRequestTarget ajax)
	{
		// getItemCount may be cached, but we need an accurate count here,
		// hence we use the internal count method
		final long newItemCount = internalGetItemCount();

		// only do anything if we actually have new items:
		long unrenderedItemCount = newItemCount - lastItemCount;
		if (unrenderedItemCount > 0)
		{
			final long firstPageWithNewItems =
					lastItemCount / getItemsPerPage();
			if (getCurrentPage() != firstPageWithNewItems || lastItemCount == 0)
			{
				// not on the first page that contains new items -> just go to
				// that page
				ajax.add(getParent());
				appending = true;
				setCurrentPage(firstPageWithNewItems);
				onPageChangeAfterAppend(ajax);
			}
			else
			{
				final long itemCountOnLastPage =
						lastItemCount % getItemsPerPage();
				final long lastRowCount = lastItemCount / getColumns();
				final long rowCountOnLastPage = lastRowCount % getRows();
				final long unusedRowsOnLastPage = getRows() -
						rowCountOnLastPage;
				long availableSlots = getItemsPerPage() - itemCountOnLastPage;
				final Iterator<AppendableItem> emptyItemsToReplace =
						renderedEmptyItems.values().iterator();
				final Iterator<IModel<T>> unrenderedItemModels =
						getItemModels(lastItemCount, unrenderedItemCount);
				int newlyRenderedItemCount = 0;
				int index = (int) (lastItemCount - 1);
				while (availableSlots > 0 &&
						unrenderedItemModels.hasNext() && emptyItemsToReplace
						.hasNext())
				{
					// first fill in the empty cells that were left after
					// the last rendering
					final IModel<T> model = unrenderedItemModels.next();
					final AppendableItem emptyItem =
							emptyItemsToReplace.next();
					emptyItemsToReplace.remove();
					final AppendableItem newItem =
							newItem(emptyItem.getId(), index, model);
					populateItem(newItem);
					emptyItem.replaceWith(newItem);
					ajax.add(newItem);
					onAppendItem(newItem, ajax);
					availableSlots--;
					index++;
					newlyRenderedItemCount++;
					unrenderedItemCount--;
					lastRenderedRowMarkupId = newItem.findParent(AppendableRowItem.class)
					                                 .getMarkupId();
				}
				if (availableSlots > 0 && unrenderedItemModels.hasNext() &&
						unusedRowsOnLastPage > 0)
				{
					// there are items left to render, but now we need to
					// create new rows
					final Iterator<IModel<T>> remainingItemModels =
							getItemModels(
									lastItemCount + newlyRenderedItemCount,
									availableSlots);
					Iterator<Item<T>> newItems =
							getItemReuseStrategy().getItems(newItemFactory(),
									remainingItemModels,
									getItems());
					addItems(newItems);
					for (AppendableRowItem newlyAddedRow : newlyAddedRows)
					{
						if (rowTagName == null)
						{
							rowTagName = newlyAddedRow
									.getItemTagName();
						}
						ajax.prependJavaScript(String.format(
								"AppendableListView.appendAfter('%s', '%s', '%s');",
								lastRenderedRowMarkupId, newlyAddedRow
										.getMarkupId(), rowTagName));
						ajax.add(newlyAddedRow);
						onAppendRow(newlyAddedRow, ajax);
						lastRenderedRowMarkupId = newlyAddedRow.getMarkupId();
					}
				}
				if (unrenderedItemCount > availableSlots)
				{
					onPageChangeAfterAppend(ajax);
				}
			}
			lastItemCount = newItemCount;
			newlyAddedRows = null;
		}
	}

	protected void onPageChangeAfterAppend(AjaxRequestTarget ajax)
	{

	}

	protected void onAppendRow(AppendableRowItem row, AjaxRequestTarget
			ajax)
	{

	}

	protected void onAppendItem(AppendableItem item, AjaxRequestTarget ajax)
	{

	}

	@SuppressWarnings("unchecked")
	@Override
	public MarkupContainer add(Component... children)
	{
		for (Component child : children)
		{
			if (AppendableRowItem.class.isAssignableFrom(child.getClass()))
			{
				if (newlyAddedRows == null)
				{
					newlyAddedRows = new ArrayList<>();
				}
				newlyAddedRows.add((AppendableRowItem) child);
			}
		}
		return super.add(children);
	}

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();
		this.lastItemCount = getItemCount();
		this.newlyAddedRows = null;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
	}

	protected class AppendableItem extends Item<T>
	{
		protected AppendableItem(String id, int index, IModel<T> model)
		{
			super(id, index, model);
			setOutputMarkupId(true);
		}

		protected AppendableItem(String id, int index)
		{
			this(id, index, null);
		}

		@Override
		protected void onAfterRender()
		{
			super.onAfterRender();
			if (this.getModel() == null)
			{
				renderedEmptyItems.put(this.getIndex(), this);
			}
		}
	}

	protected class AppendableRowItem extends Item<Object>
	{
		protected AppendableRowItem(String id, int index)
		{
			super(id, index);
			setOutputMarkupId(true);
		}

		@Override
		protected void onRender()
		{
			super.onRender();
			lastRenderedRowMarkupId = this.getMarkupId();
		}

		String getItemTagName()
		{
			final MarkupStream markupStream = new MarkupStream(getMarkup());
			return markupStream.getTag().getName();
		}
	}
}
