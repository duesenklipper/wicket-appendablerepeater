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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
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
	private static final ResourceReference SCRIPT = new PackageResourceReference(AppendableListView.class,
		"AppendableListView.js");

	// a reference to the last rendered child. This is used to get the markupId of the element after which the new
	// one should be rendered.
	private AppendableListItem lastChild;

	// elements that were added during a full repaint (i.e. when the list was initially empty).
	// see #populateItem
	private List<T> newElements;
	private String itemTagName;

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
		if (newElements != null && newElements.contains(item.getModelObject()))
		{
			// if this is an ajax request and we have newElements, that means it's a
			// full repaint for this repeater and we should give these new elements
			// the opportunity to be animated.
			AjaxRequestTarget ajax = RequestCycle.get().find(AjaxRequestTarget.class);
			if (ajax != null)
			{
				onAppendItem((AppendableListItem) item, ajax);
			}
		}
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

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();
		newElements = null;
	}

	protected abstract void populateItem(AppendableListItem item);

	/**
	 * Append <code>newElement</code> to the end of the model list, create a new ListItem for it, and render it via
	 * AJAX. For further actions on the new ListItem, see {@link #onAppendItem(AppendableListItem, AjaxRequestTarget)}.
	 * If <code>ajax</code> is null, then <code>newElement</code> is simply added to the model list, the
	 * non-ajax full-page rendering will render it as well. If <code>ajax</code> is present but the list was empty so
	 * far, then the {@linkplain AppendableListView}'s parent is rendered via AJAX. Otherwise, only the new ListItem
	 * is rendered.
	 *
	 * @param newElement The new list element
	 * @param ajax       The ajax request target
	 * @return this, for method chaining
	 */
	public AppendableListView<T> appendNewItemFor(T newElement, AjaxRequestTarget ajax)
	{
		if (getModel().getObject().isEmpty())
		{
			// if we currently have no list elements, then whatever was the last element is now stale and we need to
			// repaint anyway.
			lastChild = null;
		}
		getModel().getObject().add(newElement);
		if (ajax != null)
		{
			if (lastChild == null)
			{
				ajax.add(getParent());
				if (newElements == null)
				{
					newElements = new ArrayList<>();
				}
				newElements.add(newElement);
			}
			else
			{
				final int newIndex = getModel().getObject().size() - 1;
				final AppendableListItem newItem = newItem(newIndex, getListItemModel(getModel(), newIndex));
				add(newItem);
				populateItem(newItem);
				onAppendItem(newItem, ajax);
				if (itemTagName == null)
				{
					itemTagName = newItem.getItemTagName();
				}
				ajax.prependJavaScript(String.format("AppendableListView.appendAfter('%s', '%s', '%s');", lastChild
						.getMarkupId(), newItem
						.getMarkupId(), itemTagName));
				ajax.add(newItem);
				lastChild = newItem;
			}
		}
		return this;
	}

	/**
	 * Perform any special actions that need to be done on a ListItem being appended in an AJAX call. This could be used
	 * to e.g. add fade-in animations or other such things. This method is called by
	 * {@link #appendNewItemFor(Object, AjaxRequestTarget)} after the new ListItem is populated by
	 * {@link #populateItem(AppendableListItem)}. It is not called if {@linkplain #appendNewItemFor} was called
	 * without an {@linkplain AjaxRequestTarget}.
	 *
	 * @param newItem The new ListItem to be appended
	 * @param ajax    The ajax request target
	 */
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

		public String getItemTagName()
		{
			final MarkupStream markupStream = new MarkupStream(getMarkup());
			return markupStream.getTag().getName();
		}
	}
}
