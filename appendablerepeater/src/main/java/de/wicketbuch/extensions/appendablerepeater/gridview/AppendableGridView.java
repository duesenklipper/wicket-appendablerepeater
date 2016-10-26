package de.wicketbuch.extensions.appendablerepeater.gridview;

import static de.wicketbuch.extensions.appendablerepeater.listview.AppendableListView.SCRIPT;
import static sun.swing.MenuItemLayoutHelper.max;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

public abstract class AppendableGridView<T> extends GridView<T> implements
		IHeaderContributor
{

	private final SortedMap<Integer, AppendableItem> renderedEmptyItems = new
			TreeMap<>();
	private List<AppendableRowItem> newlyAddedRows;
	private int lastItemCount = 0;
	private int lastRenderedIndex;
	private String rowTagName;
	private String lastRowId;

	public AppendableGridView(String id, IDataProvider dataProvider)
	{
		super(id, dataProvider);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		getParent().setOutputMarkupId(true);
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		renderedEmptyItems.clear();
		lastRenderedIndex = 0;
		if (newlyAddedRows != null)
		{
			AjaxRequestTarget ajax = AjaxRequestTarget.get();
			if (ajax != null)
			{
				for (AppendableRowItem newlyAddedRow : newlyAddedRows)
				{
					onAppendRow(newlyAddedRow, ajax);
				}
			}
		}
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

	public boolean itemsAppended(AjaxRequestTarget ajax)
	{
		final int lastPage = getPageCount() - 1;
		final int newItemCount = internalGetItemCount();
		int unrenderedItemCount = newItemCount - lastItemCount;
		final int itemsPerPage = getRows() * getColumns();
		if (unrenderedItemCount > 0)
		{
			final int firstPageWithNewItems =
					lastItemCount / itemsPerPage;
			if (getCurrentPage() != firstPageWithNewItems || lastItemCount == 0)
			{
				// not on the first page that contains new items -> just go to
				// that page
				ajax.addComponent(getParent());
				setCurrentPage(firstPageWithNewItems);
				return false;
			}
			else
			{
				final int itemCountOnLastPage =
						lastItemCount % itemsPerPage;
				final int lastRowCount = lastItemCount / getColumns();
				final int rowCountOnLastPage = lastRowCount % getRows();
				final int unusedRowsOnLastPage = getRows() -
						rowCountOnLastPage;
				int availableSlots = itemsPerPage - itemCountOnLastPage;
				final Iterator<AppendableItem> emptyItemsToReplace =
						renderedEmptyItems.values().iterator();
				final Iterator<IModel<T>> unrenderedItemModels =
						getItemModels(lastItemCount, unrenderedItemCount);
				int newlyRenderedItemCount = 0;
				int index = lastRenderedIndex;
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
					ajax.addComponent(newItem);
					onAppendItem(newItem, ajax);
					availableSlots--;
					index++;
					newlyRenderedItemCount++;
					unrenderedItemCount--;
					lastRowId = newItem.findParent(AppendableRowItem.class)
					                   .getMarkupId();
				}
				if (availableSlots > 0 && unrenderedItemModels.hasNext() &&
						unusedRowsOnLastPage > 0)
				{
					// there are items left to render, but now we need to
					// create new rows
					final Iterator<IModel<T>> remainingItemModels =
							getItemModels(lastItemCount + newlyRenderedItemCount,
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
						ajax.prependJavascript(String.format(
								"AppendableListView.appendAfter('%s', '%s', '%s');",
								lastRowId, newlyAddedRow
										.getMarkupId(), rowTagName));
						ajax.addComponent(newlyAddedRow);
						onAppendRow(newlyAddedRow, ajax);
						lastRowId = newlyAddedRow.getMarkupId();
					}
				}
			}
			lastItemCount = newItemCount;
			newlyAddedRows = null;
			return true;
		}
		else
		{
			return false;
		}
	}

	protected void onAppendRow(AppendableRowItem row, AjaxRequestTarget
			ajax)
	{

	}

	protected void onAppendItem(AppendableItem item, AjaxRequestTarget ajax)
	{

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
		response.renderJavascriptReference(SCRIPT);
	}
	/**
	 * Created by calle on 24.06.16.
	 */
	protected class AppendableItem extends Item<T>
	{
		public AppendableItem(String id, int index, IModel<T> model)
		{
			super(id, index, model);
			setOutputMarkupId(true);
		}

		public AppendableItem(String id, int index)
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
			else
			{
				lastRenderedIndex = max(lastRenderedIndex, this.getIndex());
			}
		}
	}

	protected class AppendableRowItem extends Item<Object>
	{
		public AppendableRowItem(String id, int index)
		{
			super(id, index);
			setOutputMarkupId(true);
		}

		@Override
		protected void onRender(MarkupStream ms)
		{
			super.onRender(ms);
			lastRowId = this.getMarkupId();
		}

		public String getItemTagName()
		{
			final MarkupStream markupStream = this.locateMarkupStream();
			return markupStream.getTag().getName();
		}

		@Override
		protected void onInitialize()
		{
			super.onInitialize();
			if (newlyAddedRows == null)
			{
				newlyAddedRows = new ArrayList<>();
			}
			newlyAddedRows.add(this);
		}
	}
}
