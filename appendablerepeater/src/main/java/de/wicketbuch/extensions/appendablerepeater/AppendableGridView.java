/**
 * Copyright (C) 2016 Carl-Eric Menzel <cmenzel@wicketbuch.de>
 * and possibly other appendablerepeater contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wicketbuch.extensions.appendablerepeater;


import static de.wicketbuch.extensions.appendablerepeater.AppendableListView.SCRIPT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

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
	 * @see #onBeforeRender()
	 */
	private boolean appending = false;

	/**
	 * RowItems that are added (via {@link #add(Component...)}) are
	 * registered here so they can be submitted to
	 * {@link #onAppendRow(AppendableRowItem, AjaxRequestTarget)}.
	 * This list is discarded after its contents were used.
	 */
	private List<AppendableRowItem> appendedRows;

	/**
	 * Items that are generated during an append situation when we are moving to
	 * a new page are registered here, so they can be submitted to {@link
	 * #onAppendItem(AppendableItem, AjaxRequestTarget)}. This list is discarded
	 * after its contents are used.
	 */
	private List<AppendableItem> appendedItems;

	/**
	 * When appending and moving to a new page, this counts how many rows had
	 * been previously on the target page. These pre-existing rows do not need
	 * to be animated, because we only want to animate new rows, so we use this
	 * counter to skip them.
	 */
	private long preExistingRows = 0;

	/**
	 * When appending and moving to a new page, this counts how many items had
	 * been previously in the last non-"full" row. These pre-existing items do
	 * not need to be animated, because we only want to animate new items, so we
	 * use this counter to skip them.
	 */
	private long preExistingItems = 0;

	/**
	 * The number of items in the DataProvider after the last render. This is
	 * used to determine the number of new items in
	 * {@link #itemsAppended(AjaxRequestTarget)}.
	 */
	private int lastItemCount = 0;

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
	 * When rendering partially (such as when appending items, or when
	 * jumping to a new page within the grid), GridView restarts the item
	 * index at 0, which is not useful. We track the next index to use here.
	 * It is set in {@link #onBeforeRender()} when rendering the entire
	 * current page, and in {@link #itemsAppended(AjaxRequestTarget)} for
	 * AJAX appends.
	 */
	private int nextIndex;

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
		// this is a full re-render. let's figure out the first displayed index
		// so everything can be numbered correctly.
		// this needs to happen before super.onBeforeRender, so the items
		// created by super can get the correct index.
		this.nextIndex = getCurrentPage() * getItemsPerPage();
		if (nextIndex < 0)
		{
			// overflowing to more than maxint elements? we're in trouble
			// anyway.
			nextIndex = 0;
		}

		// let super create all the needed items.
		super.onBeforeRender();

		// empty items from the last render will be discarded anyway now, so
		// we don't need to track them anymore
		renderedEmptyItems.clear();

		if (appending)
		{
			// this.appending is true when #itemsAppended was running and
			// decided that we need to jump to a different page within the
			// grid. so we are rendering the entire gridview, but some of the
			// items are newly appended.
			// we are interested in newly added rows only if we are currently
			// appending. if we are not appending, rows were added in the normal
			// process of rebuilding the repeater - so we do not need the
			// appending animation.
			AjaxRequestTarget ajax = AjaxRequestTarget.get();
			// only animate if we are actually in an ajax request
			if (ajax != null)
			{
				// do we have new items that were placed in a pre-existing row?
				if (appendedItems != null)
				{
					// animate them individually
					for (AppendableItem appendedItem : appendedItems)
					{
						onAppendItem(appendedItem, ajax);
					}
				}

				// do we have new rows that were added?
				if (appendedRows != null)
				{
					// animate them row by row
					for (AppendableRowItem newlyAddedRow : appendedRows)
					{
						onAppendRow(newlyAddedRow, ajax);
					}
				}
			}
		}
		// we are done with appending now, reset the counters and buffers
		preExistingItems = 0;
		preExistingRows = 0;
		appendedItems = null;
		appendedRows = null;
		appending = false;
	}

	/**
	 * Create an empty item. You won't normally need to override this.
	 * <strong>Warning:</strong> Due to superclass limitations, the index
	 * parameter will be out-of-sync with other elements. Do not rely on it.
	 * The index for non-empty items ({@link #newItem(String, int, IModel)})
	 * will be correct, however.
	 */
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

	/**
	 * Create a new RowItem. You won't normally need to override this.
	 * <strong>Warning:</strong> Due to superclass limitations, the index
	 * parameter will be potentially out-of-order, e.g. restarting at 0. Do
	 * not rely on it.
	 */
	@Override
	protected AppendableRowItem newRowItem(String id, int index)
	{
		return new AppendableRowItem(id, index);
	}


	@Override
	protected IItemFactory<T> newItemFactory()
	{
		return new IItemFactory<T>()
		{
			@Override
			public Item<T> newItem(int index, IModel<T> model)
			{
				String id = AppendableGridView.this.newChildId();
				Item<T> item = AppendableGridView.this.newItem(id,
						AppendableGridView.this.nextIndex, model);
				AppendableGridView.this.nextIndex += 1;
				AppendableGridView.this.populateItem(item);

				return item;
			}
		};

	}

	/**
	 * Call this method after adding items to the {@code DataProvider} given to
	 * this {@code AppendableGridView}. It will then appropriately insert the
	 * new elements via ajax.
	 *
	 * @param ajax the currently active {@link AjaxRequestTarget}
	 */
	public void itemsAppended(AjaxRequestTarget ajax)
	{
		// getItemCount may be cached, but we need an accurate count here,
		// hence we use the internal count method
		final int newItemCount = internalGetItemCount();

		// only do anything if we actually have new items:
		int unrenderedItemCount = newItemCount - lastItemCount;
		if (unrenderedItemCount > 0)
		{
			final int firstPageWithNewItems =
					lastItemCount / getItemsPerPage();
			final int itemCountOnLastPage =
					lastItemCount % getItemsPerPage();
			final int lastRowCount;
			{
				int rowCount = lastItemCount / getColumns();
				if (lastItemCount % getColumns() > 0)
				{
					// partial rows count too:
					rowCount += 1;
				}
				lastRowCount = rowCount;
			}

			// number of rows that are already present on the most recently
			// rendered page
			final long rowCountOnLastPage;
			{
				long count = lastRowCount % getRows();
				if (count == 0 && lastItemCount > 0)
				{
					// no partially filled page means the last page was full
					// of rows! unless there were no items at all, then there
					// were no rows either, duh.
					count = getRows();
				}
				rowCountOnLastPage = count;
			}

			// number of row slots on the most recent page that are not yet
			// used
			final long unusedRowsOnLastPage = getRows() -
					rowCountOnLastPage;

			if (getCurrentPage() != firstPageWithNewItems || lastItemCount == 0)
			{
				// we are not on the first page that contains new items
				// -> just go to that page
				setCurrentPage(firstPageWithNewItems);

				appending = true;

				// redraw from parent
				ajax.add(getParent());

				// this many rows are already on that page and thus do not
				// need to be animated
				this.preExistingRows = rowCountOnLastPage;

				// this many items were in the last non-full page and thus do
				// not need to be animated
				this.preExistingItems = itemCountOnLastPage;

				// let the world know we changed the current page so they can
				// redraw any pager they might have
				onPageChangeAfterAppend(ajax);
			}
			else
			{
				// we are indeed on the page that will show the new items, so
				// let's add them here

				// this many slots can receive new items
				int availableSlotsInPage =
						getItemsPerPage() - itemCountOnLastPage;

				// partially-filled rows are filled with empty items. we can
				// target them individually with regular Wicket ajax, no
				// other javascript shenanigans are needed.
				final Iterator<AppendableItem> emptyItemsToReplace =
						renderedEmptyItems.values().iterator();

				final int availableSlotsInLastRow = renderedEmptyItems.size();

				int newlyRenderedItemCount = 0;
				nextIndex = (int) (lastItemCount);
				if (availableSlotsInLastRow > 0)
				{
					// create the models for the new items, but only as many as
					// we need for the last partially-filled row
					final Iterator<IModel<T>> unrenderedItemModels =
							getItemModels(lastItemCount,
									availableSlotsInLastRow);


					// first fill in the empty cells that were left after the
					// last rendering
					while (availableSlotsInPage > 0 &&
							unrenderedItemModels.hasNext() &&
							emptyItemsToReplace
									.hasNext())
					{
						final IModel<T> model = unrenderedItemModels.next();
						final AppendableItem emptyItem =
								emptyItemsToReplace.next();
						emptyItemsToReplace.remove();
						final AppendableItem newItem =
								newItem(emptyItem.getId(), nextIndex, model);
						populateItem(newItem);
						emptyItem.replaceWith(newItem);
						ajax.add(newItem);
						onAppendItem(newItem, ajax);
						availableSlotsInPage--;
						nextIndex++;
						newlyRenderedItemCount++;
						unrenderedItemCount--;
						lastRenderedRowMarkupId =
								newItem.findParent(AppendableRowItem.class)
								       .getMarkupId();
					}
				}

				if (availableSlotsInPage > 0 && unrenderedItemCount > 0 &&
						unusedRowsOnLastPage > 0)
				{
					// there are items left to render, but now we need to
					// create new rows
					final Iterator<IModel<T>> unrenderedItemModels =
							getItemModels(
									lastItemCount + newlyRenderedItemCount,
									availableSlotsInPage);

					// we will use GridView's addItems for this, which wants
					// an iterator containing the actual items, so we let the
					// ReuseStrategy build that for us
					Iterator<Item<T>> newItems =
							getItemReuseStrategy().getItems(newItemFactory(),
									unrenderedItemModels,
									getItems());
					addItems(newItems);

					for (AppendableRowItem newlyAddedRow : appendedRows)
					{
						if (rowTagName == null)
						{
							rowTagName = newlyAddedRow
									.getItemTagName();
						}
						// each row that wasn't there before needs to have an
						// element with its id inserted into the DOM, so that
						// wicket-ajax has a target to replace. since this is
						// the same that AppendableListView does we simply
						// re-use its javascript function here. see
						// AppendableListView for details.
						ajax.prependJavaScript(String.format(
								"AppendableListView.appendAfter('%s', '%s', '%s');",
								lastRenderedRowMarkupId, newlyAddedRow
										.getMarkupId(), rowTagName));
						ajax.add(newlyAddedRow);
						onAppendRow(newlyAddedRow, ajax);
						lastRenderedRowMarkupId = newlyAddedRow.getMarkupId();
					}
				}
				if (unrenderedItemCount > availableSlotsInPage)
				{
					// we have added items on the current page, but there are
					// enough items to flow over to a new page, so we tell
					// the outside world to refresh its pagers if it has any.
					onPageChangeAfterAppend(ajax);
				}
			}
			lastItemCount = newItemCount;
			appendedRows = null;
			appendedItems = null;
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
				// we only want to animate completely new rows. GridView adds
				// rows in on-screen order, so to skip the ones that were
				// already seen we can simply count down preExistingRows.
				if (preExistingRows > 0)
				{
					preExistingRows--;
				}
				else
				{
					// rows that are really new will be recorded in-order so
					// that onBeforeRender can animate them.
					if (appendedRows == null)
					{
						appendedRows = new ArrayList<>();
					}
					appendedRows.add((AppendableRowItem) child);
				}
			}
		}
		return super.add(children);
	}

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();
		this.lastItemCount = getItemCount();
		this.appendedRows = null;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.renderJavaScriptReference(SCRIPT);
	}

	protected class AppendableItem extends Item<T>
	{
		@SuppressWarnings("WeakerAccess")
		protected AppendableItem(String id, int index, IModel<T> model)
		{
			super(id, index, model);
			setOutputMarkupId(true);
		}

		@SuppressWarnings("WeakerAccess")
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
				// this is an empty item used to fill up the empty slots in a
				// non-full row. we record these items here so we can later
				// easily replace them via ajax.
				renderedEmptyItems.put(this.getIndex(), this);
			}
		}

		@Override
		protected void onInitialize()
		{
			super.onInitialize();
			recordAppendedItemIfNecessary();
		}

		private void recordAppendedItemIfNecessary()
		{
			if (this.getModel() != null)
			{
				// same as with rows (see AppendableGridView#add(Component))
				// we want to only animate those items whose models were just
				// added. so we skip the first n of them.
				if (preExistingItems > 0)
				{
					preExistingItems--;
				}
				else
				{
					if (appendedItems == null)
					{
						appendedItems = new ArrayList<>();
					}
					appendedItems.add(this);
				}
			}
		}
	}

	protected class AppendableRowItem extends Item<Object>
	{
		@SuppressWarnings("WeakerAccess")
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
