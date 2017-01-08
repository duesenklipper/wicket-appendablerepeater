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

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigationIncrementLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;

public class AjaxListViewTest
{
	public static class TestPageBasic extends WebPage
	{
		final AjaxListView<String> underTest;

		public TestPageBasic()
		{
			final ArrayList<String> strings = new ArrayList<>(Arrays.asList("foo", "bar", "baz"));
			underTest = new AjaxListView<String>("listview", new ListModel<>(strings))
			{
				@Override
				protected void populateItem(final AjaxListItem item)
				{
					item.add(new Label("index", new PropertyModel<>(item, "index")));
					item.add(new Label("content", item.getModel()));
					item.add(new Link<Void>("setfeedback")
					{
						@Override
						public void onClick()
						{
							item.info("reused");
						}
					});
				}
			};
			add(underTest);
			add(new FeedbackPanel("feedback"));
		}
	}

	public static class TestPagePaging extends WebPage
	{
		final AjaxListView<String> underTest;

		public TestPagePaging()
		{
			final ArrayList<String> strings = new ArrayList<>(Arrays.asList("foo", "bar", "baz", "quux", "quork",
					"xyzzyz"));
			underTest = new AjaxListView<String>("listview", new ListModel<>(strings), 3)
			{
				@Override
				protected void populateItem(final AjaxListItem item)
				{
					item.add(new Label("index", new PropertyModel<>(item, "index")));
					item.add(new Label("content", item.getModel()));
					item.add(new Link<Void>("setfeedbackandjump")
					{
						@Override
						public void onClick()
						{
							item.info("reused");
							setCurrentPage(0);
						}
					});
				}
			};
			add(underTest);
			add(new FeedbackPanel("feedback"));
			add(new PagingNavigationIncrementLink<Void>("nextpage", underTest, 1));
		}
	}

	@Test
	public void renders() throws Exception
	{
		final WicketTester tester = new WicketTester();
		tester.startPage(TestPageBasic.class);
		tester.assertContains(".*0.*foo.*1.*bar.*2.*baz");
	}

	@Test
	public void rendersWithReuse() throws Exception
	{
		final WicketTester tester = new WicketTester();
		final TestPageBasic page = new TestPageBasic();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContains(".*0.*foo.*1.*bar.*2.*baz");
	}

	@Test
	public void rendersWithPaging() throws Exception
	{
		final WicketTester tester = new WicketTester();
		final TestPagePaging page = new TestPagePaging();
		tester.startPage(page);
		tester.dumpPage();
		tester.assertContains(".*0.*foo.*1.*bar.*2.*baz");
		tester.assertContainsNot("quux");
		tester.clickLink("nextpage", false);
		tester.assertContains(".*3.*quux.*4.*quork.*5.*xyzzyz");
		tester.assertContainsNot("foo");
	}

	@Test
	public void rendersWithPagingAndReuse() throws Exception
	{
		final WicketTester tester = new WicketTester();
		final TestPagePaging page = new TestPagePaging();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.dumpPage();
		tester.assertContains(".*0.*foo.*1.*bar.*2.*baz");
		tester.assertContainsNot("quux");
		tester.clickLink("nextpage", false);
		tester.assertContains(".*3.*quux.*4.*quork.*5.*xyzzyz");
		tester.assertContainsNot("foo");
	}

	@Test
	public void reusesItems() throws Exception
	{
		final WicketTester tester = new WicketTester();
		final TestPageBasic page = new TestPageBasic();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContainsNot("reused");
		tester.clickLink("listview:1:setfeedback", false);
		tester.assertContains("reused");
		tester.dumpPage();
	}

	@Test
	public void reusesItemsWithPaging() throws Exception
	{
		final WicketTester tester = new WicketTester();
		final TestPagePaging page = new TestPagePaging();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContainsNot("reused");
		// go to second page
		tester.clickLink("nextpage", false);
		tester.debugComponentTrees();
		// set feedback on component on second page and jump back to first page
		tester.clickLink("listview:4:setfeedbackandjump", false);
		// should not have this feedback now
		tester.assertContainsNot("reused");
		// go to second page again
		tester.clickLink("nextpage", false);
		// now the feedback should be there if the component was reused
		tester.assertContains("reused");
		tester.dumpPage();
	}
}
