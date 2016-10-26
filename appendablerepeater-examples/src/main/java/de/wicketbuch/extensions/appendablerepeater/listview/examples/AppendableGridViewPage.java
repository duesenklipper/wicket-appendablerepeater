/**
 * Copyright (C) 2016 Carl-Eric Menzel <cmenzel@wicketbuch.de>
 * and possibly other appendablerepeater contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wicketbuch.extensions.appendablerepeater.listview.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.wicketbuch.extensions.appendablerepeater.gridview.AppendableGridView;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by calle on 19/04/16.
 */
public class AppendableGridViewPage extends WebPage
{
	private int counter = 4;

	public AppendableGridViewPage()
	{
		final List<Integer> list = new ArrayList<>();
		list.addAll(Arrays.asList(1, 2, 3));
		final AppendableGridView<Integer> appendableGridView = new
				AppendableGridView<Integer>("grid", new
						IDataProvider<Integer>()
						{
							@Override
							public Iterator iterator(long first, long count)
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
							public long size()
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
						})
				{
					@Override
					protected void populateEmptyItem(Item<Integer> item)
					{
						item.add(new Label("value", ""));
					}

					@Override
					protected void populateItem(Item<Integer> item)
					{
						item.add(new Label("value", item.getModel()));
					}

					@Override
					protected void onAppendItem(AppendableItem item,
					                            AjaxRequestTarget ajax)
					{
						item.add(new AttributeAppender("style", "display:none;",
								";")
						{
							@Override
							public boolean isTemporary(Component component)
							{
								return true;
							}
						});
						ajax.appendJavaScript(
								String.format("$('#%s').fadeIn();",
										item.getMarkupId()));
					}

					@Override
					protected void onAppendRow(AppendableRowItem row,
					                           AjaxRequestTarget ajax)
					{
						row.add(new AttributeAppender("style", "display:none;",
								";")
						{
							@Override
							public boolean isTemporary(Component component)
							{
								return true;
							}
						});
						ajax.appendJavaScript(
								String.format("$('#%s').fadeIn();",
										row.getMarkupId()));
					}
				};
		appendableGridView.setColumns(5);
		add(appendableGridView);
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
//		WebMarkupContainer container = new WebMarkupContainer("container");
//		container.setOutputMarkupId(true);
//		add(container);
//		final AppendableListView<Integer> appendableListViewEmpty = new AppendableListView<Integer>("repeaterEmpty", new ArrayList<Integer>())
//		{
//			@Override
//			protected void populateItem(final AppendableListItem item)
//			{
//				item.add(new Label("index", item.getModelObject()));
//				item.add(new Label("timestamp", new AbstractReadOnlyModel<String>()
//				{
//					@Override
//					public String getObject()
//					{
//						return DateFormat.getTimeInstance(DateFormat.LONG).format(new Date());
//					}
//				}));
//			}
//
//			@Override
//			protected void onAppendItem(AppendableListItem newItem, AjaxRequestTarget ajax)
//			{
//				newItem.add(new AttributeAppender("style", "display:none;", ";")
//				{
//					@Override
//					public boolean isTemporary(Component component)
//					{
//						return true;
//					}
//				});
//				ajax.appendJavaScript(String.format("$('#%s').fadeIn();", newItem.getMarkupId()));
//			}
//		};
//		container.add(appendableListViewEmpty);
//		add(new AjaxLink<Void>("appendEmptySingle")
//		{
//			@Override
//			public void onClick(AjaxRequestTarget ajax)
//			{
//				appendableListViewEmpty.appendNewItemFor(counter++, ajax);
//			}
//		});
//		add(new AjaxLink<Void>("appendEmptyMultiple")
//		{
//			@Override
//			public void onClick(AjaxRequestTarget ajax)
//			{
//				appendableListViewEmpty.appendNewItemFor(counter++, ajax);
//				appendableListViewEmpty.appendNewItemFor(counter++, ajax);
//				appendableListViewEmpty.appendNewItemFor(counter++, ajax);
//			}
//		});
//		add(new Link<Void>("clear")
//		{
//			@Override
//			public void onClick()
//			{
//				appendableListViewEmpty.getModelObject().clear();
//			}
//		});
	}

}
