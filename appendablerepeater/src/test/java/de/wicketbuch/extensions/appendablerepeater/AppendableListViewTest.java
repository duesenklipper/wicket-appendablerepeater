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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;

public class AppendableListViewTest
{
	@Test
	public void renders() throws Exception
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		tester.assertContains("test_0");
		tester.assertContains("test_1");
		tester.assertContains("test_2");
	}

	@Test
	public void appends() throws Exception
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(3));
		tester.clickLink("append", true);
		String lastResponse = tester.getLastResponseAsString();
		assertTrue("ajax response should contain new item", lastResponse.contains("<span wicket:id=\"label\">test_3</span>"));
		assertFalse("ajax response should not contain old items", lastResponse.contains("test_1"));
	}

	@Test
	public void repaintsCompletelyForFirstItem() throws Exception
	{
		WicketTester tester = new WicketTester();
		tester.startPage(new TestPage(0));
		tester.assertContainsNot("test_");
		tester.clickLink("append", true);
		tester.assertComponentOnAjaxResponse("container");
		tester.assertContains("test_0");
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
			final AppendableListView<Integer> underTest = new AppendableListView<Integer>("underTest", list)
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
		}
	}
}
