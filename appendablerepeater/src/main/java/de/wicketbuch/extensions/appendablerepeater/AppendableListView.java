package de.wicketbuch.extensions.appendablerepeater;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

public abstract class AppendableListView<T> extends ListView<T>
{

	public static final HeaderItem SCRIPT = JavaScriptHeaderItem.forReference(new PackageResourceReference(AppendableListView.class, "AppendableListView.js"));

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

	public AppendableListView<T> appendNewItemFor(T newObject, AjaxRequestTarget ajax)
	{
		getModel().getObject().add(newObject);
		final int newIndex = getModel().getObject().size() - 1;
		final AppendableListItem<T> newItem = newItem(newIndex, getListItemModel(getModel(), newIndex));
		populateItem(newItem);
		add(newItem);
		ajax.prependJavaScript(String.format("AppendableListView.append("));
		ajax.add(newItem);
	}

	public class AppendableListItem<T> extends ListItem<T>
	{
		public AppendableListItem(String id, int index, IModel<T> model)
		{
			super(id, index, model);
		}

		public AppendableListItem(int index, IModel<T> model)
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
	}
}
