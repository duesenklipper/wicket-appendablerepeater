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
package de.wicketbuch.extensions.appendablerepeater.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.wicketbuch.extensions.appendablerepeater.AppendableGridView;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class AppendableGridViewPage extends WebPage
{
	private int counter = 4;

	public AppendableGridViewPage()
	{
		final List<Integer> list = new ArrayList<>();
		list.addAll(Arrays.asList(1, 2, 3));
		WebMarkupContainer container = new WebMarkupContainer
				("container");
		container.setOutputMarkupId(true);
		add(container);
		final AppendableGridView<Integer> appendableGridView = new
				AppendableGridView<Integer>("grid", new ListDataProvider(list))
				{
					@Override
					protected void populateEmptyItem(Item<Integer> item)
					{
						item.add(new Label("value", "-"));
						item.add(new Label("index", new PropertyModel<>
								(item, "index")));
					}

					@Override
					protected void populateItem(Item<Integer> item)
					{
						item.add(new Label("value", item.getModel()));
						item.add(new Label("index", new PropertyModel<>
								(item, "index")));
					}

					@Override
					protected void onAppendItem(AppendableItem item,
					                            AjaxRequestTarget ajax)
					{
						item.add(new AttributeAppender("style", Model.of("display:none;"),
								";")
						{
							@Override
							public boolean isTemporary()
							{
								return true;
							}
						});
						ajax.appendJavascript(
								String.format("$('#%s').fadeIn();",
										item.getMarkupId()));
					}

					@Override
					protected void onAppendRow(AppendableRowItem row,
					                           AjaxRequestTarget ajax)
					{
						row.add(new AttributeAppender("style", Model.of("display:none;"),
								";")
						{
							@Override
							public boolean isTemporary()
							{
								return true;
							}
						});
						ajax.appendJavascript(
								String.format("$('#%s').fadeIn();",
										row.getMarkupId()));
					}

					@Override
					protected void onPageChangeAfterAppend(AjaxRequestTarget ajax)
					{
						ajax.addComponent(AppendableGridViewPage.this.getPage().get
								("pager"));
					}
				};
		appendableGridView.setColumns(5);
		appendableGridView.setRows(3);
		container.add(appendableGridView);
		final AjaxPagingNavigator pager =
				new AjaxPagingNavigator("pager", appendableGridView);
		pager.setOutputMarkupId(true);
		add(pager);
		add(new AjaxLink<Void>("append")
		{
			@Override
			public void onClick(AjaxRequestTarget ajax)
			{
				list.add(counter++);
				appendableGridView.itemsAppended(ajax);
			}
		});
		add(new AjaxLink<Void>("appendMultiple")
		{
			@Override
			public void onClick(AjaxRequestTarget ajax)
			{
				list.add(counter++);
				list.add(counter++);
				list.add(counter++);
				appendableGridView.itemsAppended(ajax);
			}
		});

		// container is needed to have a parent around the initially empty repeater so it can be repainted
		WebMarkupContainer containerEmpty = new WebMarkupContainer
				("containerEmpty");
		containerEmpty.setOutputMarkupId(true);
		add(containerEmpty);
		final List<Integer> list2 = new ArrayList<>();
		final AppendableGridView<Integer> appendableGridViewEmpty = new
				AppendableGridView<Integer>("gridEmpty", new ListDataProvider
						(list2))
				{
					@Override
					protected void populateEmptyItem(Item<Integer> item)
					{
						item.add(new Label("value", "-"));
						item.add(new Label("index", new PropertyModel<>
								(item, "index")));
					}

					@Override
					protected void populateItem(Item<Integer> item)
					{
						item.add(new Label("value", item.getModel()));
						item.add(new Label("index", new PropertyModel<>
								(item, "index")));
					}

					@Override
					protected void onAppendItem(AppendableItem item,
					                            AjaxRequestTarget ajax)
					{
						item.add(new AttributeAppender("style", Model.of("display:none;"),
								";")
						{
							@Override
							public boolean isTemporary()
							{
								return true;
							}
						});
						ajax.appendJavascript(
								String.format("$('#%s').fadeIn();",
										item.getMarkupId()));
					}

					@Override
					protected void onAppendRow(AppendableRowItem row,
					                           AjaxRequestTarget ajax)
					{
						row.add(new AttributeAppender("style", Model.of("display:none;"),
								";")
						{
							@Override
							public boolean isTemporary()
							{
								return true;
							}
						});
						ajax.appendJavascript(
								String.format("$('#%s').fadeIn();",
										row.getMarkupId()));
					}
				};
		appendableGridViewEmpty.setColumns(5);
		containerEmpty.add(appendableGridViewEmpty);
		add(new AjaxLink<Void>("appendEmptySingle")
		{
			@Override
			public void onClick(AjaxRequestTarget ajax)
			{
				list2.add(counter++);
				appendableGridViewEmpty.itemsAppended(ajax);
			}
		});
		add(new AjaxLink<Void>("appendEmptyMultiple")
		{
			@Override
			public void onClick(AjaxRequestTarget ajax)
			{
				list2.add(counter++);
				list2.add(counter++);
				list2.add(counter++);
				appendableGridViewEmpty.itemsAppended(ajax);
			}
		});
		add(new Link<Void>("clear")
		{
			@Override
			public void onClick()
			{
				list2.clear();
			}
		});
	}

	private static class ListDataProvider implements IDataProvider<Integer>
	{
		private final List<Integer> list;

		public ListDataProvider(List<Integer> list)
		{
			this.list = list;
		}

		@Override
		public Iterator iterator(int first, int count)
		{
			int toIndex = (int) (first + count);
			if (toIndex < 0 || toIndex >= list.size()) {
				toIndex = list.size();
			}
			return list.subList((int) first,
					toIndex
			).iterator();
		}

		@Override
		public int size()
		{
			return list.size();
		}

		@Override
		public IModel model(Integer object)
		{
			return Model.of(object);
		}

		@Override
		public void detach()
		{

		}
	}
}
