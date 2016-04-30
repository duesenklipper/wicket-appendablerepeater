package de.wicketbuch.extensions.appendablerepeater;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public abstract class AppendableListView<T> extends ListView<T>
{
	public static final ResourceReference SCRIPT = new PackageResourceReference(AppendableListView.class, "AppendableListView.js");
	private AppendableListItem lastChild;

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
	protected AppendableListItem newItem(int index, IModel<T> itemModel)
	{
		return new AppendableListItem(index, itemModel);
	}

	@Override
	protected final void populateItem(ListItem<T> item)
	{
		populateItem((AppendableListItem) item);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		getParent().setOutputMarkupId(true);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
	}

	protected abstract void populateItem(AppendableListItem item);

	public AppendableListView<T> appendNewItemFor(T newObject, AjaxRequestTarget ajax)
	{
		getModel().getObject().add(newObject);
		if (lastChild == null)
		{
			ajax.add(getParent());
		}
		else
		{
			final int newIndex = getModel().getObject().size() - 1;
			final AppendableListItem newItem = newItem(newIndex, getListItemModel(getModel(), newIndex));
			add(newItem);
			populateItem(newItem);
			ajax.prependJavaScript(String.format("AppendableListView.appendAfter('%s', '%s');", lastChild.getMarkupId(), newItem
					.getMarkupId()));
			ajax.add(newItem);
		}
		return this;
	}

	public class AppendableListItem extends ListItem<T>
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

		@Override
		protected void onRender()
		{
			super.onRender();
			AppendableListView.this.lastChild = this;
		}
	}
}
