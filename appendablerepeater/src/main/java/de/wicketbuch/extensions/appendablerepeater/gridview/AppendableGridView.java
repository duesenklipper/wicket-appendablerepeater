package de.wicketbuch.extensions.appendablerepeater.gridview;

import static sun.swing.MenuItemLayoutHelper.max;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

public abstract class AppendableGridView<T> extends GridView<T>
{

	private SortedMap<Integer, AppendableItem> renderedEmptyItems;
	private int lastItemCount = 0;
	private int lastRenderedIndex;
	private AppendableRowItem lastRenderedRow;

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
		if (renderedEmptyItems != null)
		{
			renderedEmptyItems.clear();
		}
		lastRenderedIndex = 0;
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
		if (getCurrentPage() == lastPage)
		{
			// not on the last page -> just go to last page
			ajax.addComponent(getParent());
			setCurrentPage(lastPage);
			return false;
		}
		else
		{
			final int newItemCount = getItemCount();
			final int unrenderedItemCount = newItemCount - lastItemCount;
			final int itemsPerPage = getRows() * getColumns();
			if (unrenderedItemCount > 0)
			{
				final int itemCountOnLastPage = lastItemCount % itemsPerPage;
				int availableSlots = itemsPerPage - itemCountOnLastPage;
				final Iterator<AppendableItem> emptyItemsToReplace = renderedEmptyItems.values().iterator();
				final Iterator<IModel<T>> unrenderedItemModels = getItemModels(lastItemCount, unrenderedItemCount);
				int index = lastRenderedIndex;
				while (availableSlots > 0 && emptyItemsToReplace.hasNext() && unrenderedItemModels.hasNext())
				{
					final AppendableItem emptyItem = emptyItemsToReplace.next();
					final IModel<T> model = unrenderedItemModels.next();
					final AppendableItem newItem = newItem(emptyItem.getId(), index, model);
					emptyItem.replaceWith(newItem);
					ajax.addComponent(newItem);
					itemAppended(newItem, ajax);
					availableSlots--;
					index++;
				}
				final List<AppendableItem> newItems = new ArrayList<AppendableItem>();

				while (unrenderedItemModels.hasNext() && availableSlots > 0)
				{
					IModel<T> model = unrenderedItemModels.next();
					final AppendableItem newItem = newItem(newChildId(), index, model);
					newItems.add(newItem);
					itemAppended(newItem, ajax);
				}


				return true;
			}
			else
			{
				return false;
			}
		}
	}

	protected void itemAppended(AppendableItem item, AjaxRequestTarget ajax)
	{

	}

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();
		this.lastItemCount = getItemCount();
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
				if (renderedEmptyItems == null)
				{
					renderedEmptyItems = new TreeMap<>();
				}
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
		}

		@Override
		protected void onRender()
		{
			super.onRender();
			AppendableGridView.this.lastRenderedRow = this;
		}
	}
}
