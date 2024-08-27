/*
 * Copyright (C) 2016-2021 Carl-Eric Menzel <cmenzel@wicketbuch.de>
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


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

class AppendableListViewTest
{
	@Test
	void renders()
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		tester.assertContains("test_0");
		tester.assertContains("test_1");
		tester.assertContains("test_2");
	}

	@Test
	void appends()
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		tester.clickLink("append", true);
		String lastResponse = tester.getLastResponseAsString();
		assertThat(lastResponse)
				.as("ajax response should contain new item")
				.contains("<span wicket:id=\"label\">test_3</span>")
				.as("ajax response should not contain old items")
				.doesNotContain("test_1");
	}

	@Test
	void repaintsCompletelyForFirstItem()
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(0));
		tester.assertContainsNot("test_");
		tester.clickLink("append", true);
		tester.assertComponentOnAjaxResponse("container");
		tester.assertContains("test_0");
	}

	@Test
	void removesCorrectItem()
	{
		final WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		final String markupIdToBeRemoved =
				tester.getComponentFromLastRenderedPage("container:underTest:1").getMarkupId();
		tester.clickLink("remove", true);
		tester.assertContains("removeItem\\('" + markupIdToBeRemoved + "'\\)");
		tester.startPage(tester.getLastRenderedPage()); // do a full re-render
		tester.assertContains("test_0");
		tester.assertContainsNot("test_1");
		tester.assertContains("test_2");
	}

	@Test
	void doesNothingWhenRemovingNonexistentItem()
	{
		final WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		tester.clickLink("removeNonexisting", true);
		assertThat(tester.getLastResponseAsString())
				.isEqualTo(
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><ajax-response></ajax-response>");
	}

	public static class TestPage extends WebPage
	{
		private int counter = 0;

		public TestPage(int initial)
		{
			List<Integer> list = new ArrayList<>();
			for (int i = 0; i < initial; i++)
			{
				list.add(counter++);
			}
			WebMarkupContainer container = new WebMarkupContainer("container");
			add(container);
			final AppendableListView<Integer> underTest =
					new AppendableListView<>("underTest", list)
					{
						@Override
						protected void populateItem(AppendableListItem item)
						{
							item.add(new Label("label", "test_" + item.getModelObject()));
						}
					};
			container.add(underTest);
			add(new AjaxLink<Void>("append")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					underTest.appendNewItemFor(counter++, ajax);
				}
			});
			add(new AjaxLink<Void>("remove")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					underTest.removeItemFor(1, ajax);
				}
			});
			add(new AjaxLink<Void>("removeNonexisting")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					underTest.removeItemFor(42, ajax);
				}
			});
		}
	}
}
