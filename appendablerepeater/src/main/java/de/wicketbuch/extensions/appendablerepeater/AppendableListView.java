package de.wicketbuch.extensions.appendablerepeater;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

public abstract class AppendableListView<T> extends ListView<T>
{
	public AppendableListView(String id)
	{
		super(id);
	}

	public AppendableListView(String id, IModel<? extends List<T>> model)
	{
		super(id, model);
	}

	public AppendableListView(String id, List<T> list)
	{
		super(id, list);
	}

	@Override
	protected AppendableListItem<T> newItem(int index, IModel<T> itemModel)
	{
		return new AppendableListItem<T>(index, itemModel);
	}

	@Override
	protected final void populateItem(ListItem<T> item)
	{
		populateItem((AppendableListItem<T>) item);
	}

	protected abstract void populateItem(AppendableListItem<T> item);

	public class AppendableListItem<U extends T> extends ListItem<U>
	{
		public AppendableListItem(String id, int index, IModel<U> model)
		{
			super(id, index, model);
		}

		public AppendableListItem(int index, IModel<U> model)
		{
			super(index, model);
		}

		public AppendableListItem(String id, int index)
		{
			super(id, index);
		}

		@Override
		protected void onInitialize()
		{
			super.onInitialize();
			setOutputMarkupId(true);
		}

		public AppendableListItem<U> append(AjaxRequestTarget ajax, U object)
		{
			int newIndex = this.getIndex() + 1;
			IModel<? extends List<T>> listViewModel = AppendableListView.this.getModel();
			listViewModel.getObject().add(newIndex, object);
			AppendableListItem<T> newItem = newItem(newIndex, getListItemModel(listViewModel, newIndex));
			AppendableListView.this.add(newItem);
			AppendableListView.this.populateItem(newItem);

		}
	}
}
