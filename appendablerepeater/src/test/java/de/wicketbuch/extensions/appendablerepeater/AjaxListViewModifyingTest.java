package de.wicketbuch.extensions.appendablerepeater;

import java.util.ArrayList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;

public class AjaxListViewModifyingTest
{
	@Test
	public void appendWithoutAjaxRenders() throws Exception
	{
		final WicketTester tester = new WicketTester();
		tester.startPage(TestPage.class);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2");
		tester.clickLink("append", false);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2" +
				".*>3<.*>3<.*appended");
	}

	@Test
	public void appendWithoutAjaxRendersWithReuse() throws Exception
	{
		final WicketTester tester = new WicketTester();
		TestPage page = new TestPage();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2");
		tester.clickLink("append", false);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2" +
				".*>3<.*>3<.*appended");
	}

	@Test
	public void insertWithoutAjaxRenders() throws Exception
	{
		final WicketTester tester = new WicketTester();
		tester.startPage(TestPage.class);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2");
		tester.clickLink("insert1at1", false);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*insertedAt1" +
				".*>2<.*>2<.*val1" +
				".*>3<.*>3<.*val2");
	}

	@Test
	public void insertWithoutAjaxRendersWithReuse() throws Exception
	{
		final WicketTester tester = new WicketTester();
		TestPage page = new TestPage();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2");
		tester.clickLink("insert1at1", false);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*insertedAt1" +
				".*>2<.*>1<.*val1" +
				".*>3<.*>2<.*val2");
	}
	@Test
	public void insertAtMultiplePositionsWithoutAjaxRendersWithReuse() throws Exception
	{
		final WicketTester tester = new WicketTester();
		TestPage page = new TestPage();
		page.underTest.setReuseItems(true);
		tester.startPage(page);
		tester.assertContains(".*>0<.*>0<.*val0" +
				".*>1<.*>1<.*val1" +
				".*>2<.*>2<.*val2");
		tester.clickLink("insertmulti", false);
		tester.assertContains(".*>0<.*>0<.*inserted" +
				".*>1<.*>0<.*val0" +
				".*>2<.*>2<.*inserted" +
				".*>3<.*>1<.*val1" +
				".*>4<.*>4<.*inserted" +
				".*>5<.*>2<.*val2");
	}

	public static class TestPage extends WebPage
	{
		private AjaxListView<String> underTest;

		public TestPage()
		{
			final ArrayList<String> strings = new ArrayList<>();
			strings.add("val0");
			strings.add("val1");
			strings.add("val2");
			underTest = new AjaxListView<String>("listview", new ListModel<String>(strings))
			{
				@Override
				protected void populateItem(AjaxListItem item)
				{
					item.add(new Label("currentindex", new PropertyModel<>(item, "index")));
					item.add(new Label("originalindex", item.getIndex()));
					item.add(new Label("value", item.getModel()));
				}
			};
			add(underTest);
			add(new AjaxFallbackLink<Void>("append")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add("appended");
					underTest.itemsAppended(ajax);
				}
			});
			add(new AjaxFallbackLink<Void>("insert1at0")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add(0, "insertedAt0");
					underTest.itemsInsertedAt(0, 1, ajax);
				}
			});
			add(new AjaxFallbackLink<Void>("insert3at0")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add(0, "insertedAt0");
					strings.add(0, "insertedAt0");
					strings.add(0, "insertedAt0");
					underTest.itemsInsertedAt(0, 3, ajax);
				}
			});
			add(new AjaxFallbackLink<Void>("insert1at1")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add(1, "insertedAt1");
					underTest.itemsInsertedAt(1, 1, ajax);
				}
			});
			add(new AjaxFallbackLink<Void>("insert3at1")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add(1, "insertedAt1");
					strings.add(1, "insertedAt1");
					strings.add(1, "insertedAt1");
					underTest.itemsInsertedAt(1, 3, ajax);
				}
			});
			add(new AjaxFallbackLink<Void>("insertmulti")
			{
				@Override
				public void onClick(AjaxRequestTarget ajax)
				{
					strings.add(2, "inserted");
					strings.add(1, "inserted");
					strings.add(0, "inserted");
					underTest.itemsInsertedAt(2, 1, ajax);
					underTest.itemsInsertedAt(1, 1, ajax);
					underTest.itemsInsertedAt(0, 1, ajax);
				}
			});
		}
	}
}
