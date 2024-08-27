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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

class AppendableGridViewTest
{
	private final Set<String> appendedRows = new HashSet<>();
	private final Set<String> appendedItems = new HashSet<>();
	private boolean pageChangeCalled = false;

	@Test
	void renders()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(3));
		tester.assertContains("test_0");
		tester.assertContains("test_1");
		tester.assertContains("test_2");
	}

	@Test
	void appendsInExistingRow()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(2));
		tester.clickLink("append", true);
		String lastResponse = tester.getLastResponseAsString();
		assertThat(lastResponse)
				.as("ajax response should contain new item")
				.contains(
						"<span wicket:id=\"label\" path=\"container_underTest_1_cols_4_label\">test_2</span>"
				)
				.as("ajax response should not contain old items")
				.doesNotContain("test_1");
	}

	@Test
	void repaintsCompletelyForFirstItem()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(0));
		tester.assertContainsNot("test_");
		tester.clickLink("append", true);
		tester.assertComponentOnAjaxResponse("container");
		tester.assertContains("test_0");
	}

	@Test
	void appendsInNewRow()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(3));
		tester.clickLink("append", true);
		// 1 is the old row, 2-4 are the elements of the old row, 5 is the new
		// row, ajax response must contain the new row
		tester.assertComponentOnAjaxResponse("container:underTest:5");
		final String lastResponse = tester.getLastResponseAsString();
		// must not contain the old row
		assertThat(lastResponse)
				.as("ajax response should not contain old row")
				.doesNotContain("container_underTest_1")
				.as("ajax response should not contain old items")
				.doesNotContain("test_1")
				.as("ajax response should contain new item")
				.contains("test_3")
				.as("onAppendRow should have been called for new row")
				.contains("5");
		assertThat(appendedItems)
				.as("onAppendItem should not be called for item when new row is created")
				.isEmpty();
	}

	@Test
	void appendsAcrossOldAndNewRows()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(2));
		tester.clickLink("append3", true);
		// 1 is the old row, 2-4 are the elements of the old row, 5 is the new
		// row, ajax response must contain the new row
		tester.assertComponentOnAjaxResponse("container:underTest:5");
		final String lastResponse = tester.getLastResponseAsString();
		// must not contain the old row
		assertThat(lastResponse)
				.as("ajax response should not contain old row")
				.doesNotContain("\"container_underTest_1\"")
				.as("ajax response should not contain old items")
				.doesNotContain("test_1")
				.as("ajax response should contain new item 2")
				.contains("test_2")
				.as("ajax response should contain new item 3")
				.contains("test_3")
				.as("ajax response should contain new item 4")
				.contains("test_4");

		assertThat(appendedRows)
				.as("onAppendRow should have been called for new row")
				.contains("5")
				.as("onAppendRow should only be called for new row")
				.hasSize(1);

		assertThat(appendedItems)
				.as("onAppendItem should be called for new item in old row")
				.contains("2")
				.as("onAppendItem should only be called for new item in old row")
				.hasSize(1);
	}

	@Test
	void appendsOnPageAndSignalsOverflowToNextPage()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(5));
		// grid has two rows of three, so there is one slot left to be filled.
		// other two should go to the next page
		tester.clickLink("append3", true);
		final String lastResponse = tester.getLastResponseAsString();
		assertThat(lastResponse)
				.as("should contain last fitting item in response")
				.contains("test_5")
				.as("should not contain overflowing items in response").doesNotContain(
						"test_6");
		assertThat(pageChangeCalled).as("should call onPageChanged").isTrue();
		assertThat(appendedItems)
				.as("should call onAppendItem for new item on this page")
				.contains("5")
				.as("should not call onAppendItem for any other item")
				.hasSize(1);

		assertThat(appendedRows)
				.as("should not call onAppendRow at all")
				.isEmpty();

		@SuppressWarnings("unchecked") final AppendableGridView<Integer> underTest =
				(AppendableGridView<Integer>) tester.getComponentFromLastRenderedPage(
						"container:underTest");

		assertThat(underTest.getCurrentPage())
				.as("should stay on first page (index 0)")
				.isZero();
	}

	@Test
	void jumpsToNewPageAsNeeded()
	{
		WicketTester tester = newTester();
		tester.startPage(new TestPage(7));
		// grid has two rows of three, so there is one slot filled on the
		// next page already. new items should go to that page, which should
		// then be displayed.
		tester.clickLink("append3", true);
		// the parent of the gridview should be rendered because we
		// completely switch pages
		tester.assertComponentOnAjaxResponse("container");
		final String lastResponse = tester.getLastResponseAsString();
		assertThat(lastResponse)
				.as("should contain already existing items on second page")
				.contains("test_6")
				.as("should contain new items").contains("test_7")
				.contains("test_8")
				.contains("test_9")
				.as("should not contain items on previous page")
				.doesNotContain("test_2");

		assertThat(appendedItems)
				.as("should call onAppendItem for new items")
				.containsAll(Arrays.asList("7", "8", "9"))
				.as("should not call onAppendItem for existing items")
				.hasSize(3);

		@SuppressWarnings("unchecked") final AppendableGridView<Integer> underTest =
				(AppendableGridView<Integer>) tester.getComponentFromLastRenderedPage(
						"container:underTest");

		assertThat(underTest
				.getCurrentPage()).as("should be on second page (index 1) now").isEqualTo(1);
	}

	@Test
	void itemIndexContinuesCorrectly()
	{
		final WicketTester tester = newTester();
		tester.startPage(new TestPage(1));
		tester.assertContains("index_0");
		tester.clickLink("append3", true);
		tester.assertContains("index_1.*index_2.*index_3");
	}

	@Test
	void itemIndexContinuesCorrectly_Reuse_github_issue_1()
	{
		// when reusing items, appending directly into a new row (no empty
		// slots left in the last rendered row) after a non-ajax render
		// messed  up the index prior to fixing issue #1
		final WicketTester tester = newTester();
		// fill the first row using ajax
		tester.startPage(new TestPage_Reuse(1));
		tester.assertContains("index_0");
		tester.clickLink("append", true);
		tester.assertContains("index_1");
		tester.clickLink("append", true);
		tester.assertContains("index_2");
		// render without ajax
		tester.clickLink("reload", false);
		tester.assertContains
				      ("index_0.*index_1.*index_2");
		// append into the next row, should keep counting index up
		tester.clickLink("append3", true);
		tester.assertContains
				      ("index_3.*index_4.*index_5");
	}

	private WicketTester newTester()
	{
		final WicketTester tester = new WicketTester();
		tester.getApplication().getDebugSettings()
		      .setComponentPathAttributeName("path");
		return tester;
	}

	public class TestPage_Reuse extends TestPage
	{
		public TestPage_Reuse(int initial)
		{
			super(initial);
			underTest.setItemReuseStrategy(
					ReuseIfModelsEqualStrategy.getInstance());
		}
	}

	public class TestPage extends WebPage
	{
		final AppendableGridView<Integer> underTest;
		private int counter = 0;

		public TestPage(int initial)
		{
			final List<Integer> list = new ArrayList<>();
			for (int i = 0; i < initial; i++)
			{
				list.add(counter++);
			}
			WebMarkupContainer container = new WebMarkupContainer("container");
			add(container);
			IDataProvider<Integer>
					dataProvider = new ListDataProvider(list);
			underTest = new
					AppendableGridView<>("underTest", dataProvider)
					{
						@Override
						protected void populateEmptyItem(Item<Integer> item)
						{
							item.add(new Label("label", "empty"));
							item.add(new Label("index", ""));
						}

						@Override
						protected void populateItem(Item<Integer> item)
						{
							item.add(new Label("label",
									"test_" + item.getModelObject()));
							item.add(new Label("index", "index_" + item
									.getIndex()));
						}

						@Override
						protected void onAppendRow(AppendableRowItem row,
						                           AjaxRequestTarget ajax)
						{
							appendedRows.add(row.getId());
						}

						@Override
						protected void onAppendItem(AppendableItem item,
						                            AjaxRequestTarget ajax)
						{
							appendedItems.add("" + item.getModelObject());
						}

						@Override
						protected void onPageChangeAfterAppend(
								AjaxRequestTarget ajax)
						{
							pageChangeCalled = true;
						}
					};
			underTest.setColumns(3);
			underTest.setRows(2);
			container.add(underTest);
			container.add(new PagingNavigator("pager", underTest));
			add(new AjaxLink<Void>("append")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					list.add(counter++);
					underTest.itemsAppended(ajax);
				}
			});
			add(new AjaxLink<Void>("append3")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					list.add(counter++);
					list.add(counter++);
					list.add(counter++);
					underTest.itemsAppended(ajax);
				}
			});
			add(new Link<Void>("reload")
			{
				@Override
				public void onClick()
				{
					// NOP, just re-render
				}
			});
		}
	}

	private static class ListDataProvider implements IDataProvider<Integer>
	{
		private final List<Integer> list;

		public ListDataProvider(List<Integer> list)
		{
			this.list = list;
		}

		@Override
		public Iterator<Integer> iterator(long first, long count)
		{
			int toIndex = (int) (first + count);
			if (toIndex < 0 || toIndex >= list.size())
			{
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
		public IModel<Integer> model(Integer object)
		{
			return Model.of(object);
		}
	}
}
