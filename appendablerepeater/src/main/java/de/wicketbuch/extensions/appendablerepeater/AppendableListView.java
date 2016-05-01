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

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * A {@link ListView} implementation that can dynamically append items via AJAX, without repainting the entire list. It
 * is very nearly a drop-in replacement for ListView, the only breaking change is that the
 * {@link #populateItem(AppendableListItem)} populateItem} method now takes an {@link AppendableListItem} rather than a
 * plain {@link ListItem}.
 * <p>
 * To append a new item at the end of the list, call {@link #appendNewItemFor(T, AjaxRequestTarget)}.
 *
 * @param <T> The list element type
 * @author Carl-Eric Menzel cmenzel@wicketbuch.de
 */
public abstract class AppendableListView<T> extends ListView<T>
{
	public static final ResourceReference SCRIPT = new PackageResourceReference(AppendableListView.class, "AppendableListView.js");

	// a reference to the last rendered child. This is used to determine  the HTML element name to
	// synthesize a new elemen
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

	/**
	 * Append <code>newObject</code> to the end of the model list, create a new ListItem for it, and render it via
	 * AJAX.
	 *
	 * @param newObject The new list element
	 * @param ajax      The ajax request target
	 * @return this, for method chaining
	 */
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
			onAppendItem(newItem, ajax);
			ajax.prependJavaScript(String.format("AppendableListView.appendAfter('%s', '%s');", lastChild.getMarkupId(), newItem
					.getMarkupId()));
			ajax.add(newItem);
		}
		return this;
	}

	protected void onAppendItem(AppendableListItem newItem, AjaxRequestTarget ajax)
	{

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
