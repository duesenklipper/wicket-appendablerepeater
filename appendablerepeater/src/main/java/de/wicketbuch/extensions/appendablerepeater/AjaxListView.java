/**
 * Copyright (C) 2016-17 Carl-Eric Menzel <cmenzel@wicketbuch.de>
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

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.IGenericComponent;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

public abstract class AjaxListView<T> extends RepeatingView implements IGenericComponent<List<T>>, IPageableItems
{
	private long currentPage;
	private long itemsPerPage;
	private SortedMap<Integer, AjaxListItem> cachedItems;
	private AjaxListItem lastItem;

	public AjaxListView(String id)
	{
		this(id, null);
	}

	public AjaxListView(String id, IModel<List<T>> model)
	{
		this(id, model, Long.MAX_VALUE);
	}

	public AjaxListView(String id, long itemsPerPage)
	{
		this(id, null, itemsPerPage);
	}

	public AjaxListView(String id, IModel<List<T>> model, long itemsPerPage)
	{
		super(id, model);
		this.itemsPerPage = itemsPerPage;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		getParent().setOutputMarkupId(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IModel<List<T>> getModel()
	{
		return (IModel<List<T>>) getDefaultModel();
	}

	@Override
	public void setModel(IModel<List<T>> model)
	{
		setDefaultModelObject(model);
	}

	@Override
	public void setModelObject(List<T> object)
	{
		setDefaultModelObject(object);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getModelObject()
	{
		return (List<T>) getDefaultModelObject();
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		removeAll();
		final int start;
		final int end;
		{
			long s = currentPage * itemsPerPage;
			if (s > Integer.MAX_VALUE || s < 0)
			{
				start = 0;
			}
			else
			{
				start = (int) s;
			}

			final long e = start + itemsPerPage;
			if (e > getItemCount() || e < 0)
			{
				end = (int) getItemCount(); // can safely cast because our list can't be longer than maxint
			}
			else
			{
				end = (int) e;
			}
		}
		AjaxListItem previousItem = null;
		if (isReuseItems() && start > 0)
		{
			for (int index = start - 1; index >= 0; index--)
			{
				previousItem = cachedItems.get(index);
				if (previousItem != null)
				{
					break;
				}
			}
		}
		for (int index = start; index < end; index++)
		{
			final AjaxListItem item = getItemAt(index, previousItem);
			add(item);
			previousItem = item;
		}
	}

	protected AjaxListItem newItem(String id, ListItemModel model)
	{
		return new AjaxListItem(id, model);
	}

	protected ListItemModel newModel(int index)
	{
		return new ListItemModel(index);
	}

	protected abstract void populateItem(AjaxListItem item);

	public void itemsAppended(AjaxRequestTarget ajax)
	{
	}

	public void itemsInsertedAt(int insertIndex, int count, AjaxRequestTarget ajax)
	{
		if (isReuseItems())
		{
			AjaxListItem item = lastItem;
			while (item != null && item.getIndex() >= insertIndex)
			{
				cachedItems.remove(item.getIndex());
				item.getModel().setIndex(item.getIndex() + count);
				cachedItems.put(item.getIndex(), item);
				item = item.previousItem;
			}
		}
	}

	protected class ListItemModel implements IModel<T>
	{

		public int getIndex()
		{
			return index;
		}

		public void setIndex(int index)
		{
			this.index = index;
		}

		private int index;

		public ListItemModel(int index)
		{
			this.index = index;
		}

		@Override
		public T getObject()
		{
			return getList().get(index);
		}

		@Override
		public void setObject(T object)
		{
			getList().set(index, object);
		}

		@Override
		public void detach()
		{
			// NOP
		}
	}

	private AjaxListItem getItemAt(int index, AjaxListItem previousItem)
	{
		final AjaxListItem item;
		if (isReuseItems() && cachedItems.containsKey(index))
		{
			item = cachedItems.get(index);
		}
		else
		{
			ListItemModel model = newModel(index);
			item = newItem(newChildId(), model);
			populateItem(item);
			if (isReuseItems())
			{
				cachedItems.put(index, item);
			}
			if (lastItem == null || lastItem.getIndex() < index)
			{
				lastItem = item;
			}
		}
		item.previousItem = previousItem;
		return item;
	}

	protected class AjaxListItem extends WebMarkupContainer
	{
		AjaxListItem previousItem;
		AjaxListItem nextItem;

		public AjaxListItem(String id, ListItemModel model)
		{
			super(id, model);
			setOutputMarkupId(true);
		}

		public ListItemModel getModel()
		{
			return (ListItemModel) getDefaultModel();
		}

		public void setModelObject(T object)
		{
			setDefaultModelObject(object);
		}

		public T getModelObject()
		{
			return (T) getDefaultModelObject();
		}

		public int getIndex()
		{
			return getModel().getIndex();
		}
	}

	public boolean isReuseItems()
	{
		return cachedItems != null;
	}

	public void setReuseItems(boolean reuseItems)
	{
		if (reuseItems)
		{
			if (cachedItems == null)
			{
				cachedItems = new TreeMap<>();
			}
		}
		else
		{
			cachedItems = null;
		}
	}

	@Override
	public long getCurrentPage()
	{
		return currentPage;
	}

	@Override
	public void setCurrentPage(long page)
	{
		this.currentPage = page;
	}

	@Override
	public long getPageCount()
	{
		return ((getItemCount() + itemsPerPage) - 1) / itemsPerPage;
	}

	@SuppressWarnings("unchecked")
	public final List<T> getList()
	{
		final List<T> list = (List<T>) getDefaultModelObject();
		if (list == null)
		{
			return Collections.emptyList();
		}
		return list;
	}


	public long getItemCount()
	{
		return getList().size();
	}

	@Override
	public long getItemsPerPage()
	{
		return itemsPerPage;
	}

	@Override
	public void setItemsPerPage(long itemsPerPage)
	{
		this.itemsPerPage = itemsPerPage;
	}
}
