package de.wicketbuch.extensions.appendablerepeater.examples;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.wicketbuch.extensions.appendablerepeater.AppendableListView;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Created by calle on 19/04/16.
 */
public class HomePage extends WebPage
{
	private int counter = 5;

	public HomePage()
	{
		List<Integer> list = new ArrayList<>();
		list.addAll(Arrays.asList(1, 2, 3, 4));
		final AppendableListView<Integer> appendableListView = new AppendableListView<Integer>("repeater", list)
		{
			@Override
			protected void populateItem(final AppendableListItem item)
			{
				item.add(new Label("index", item.getModelObject()));
				item.add(new Label("timestamp", new AbstractReadOnlyModel<String>()
				{
					@Override
					public String getObject()
					{
						return DateFormat.getTimeInstance(DateFormat.LONG).format(new Date());
					}
				}));
			}
		};
		add(appendableListView);
		add(new AjaxLink<Void>("append")
		{
			@Override
			public void onClick(AjaxRequestTarget ajax)
			{
				appendableListView.appendNewItemFor(counter++, ajax);
			}
		});
	}

}
