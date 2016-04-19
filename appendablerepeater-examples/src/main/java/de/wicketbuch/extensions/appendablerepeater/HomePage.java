package de.wicketbuch.extensions.appendablerepeater;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
		add(new AppendableListView<Integer>("repeater", list)
		{
			@Override
			protected void populateItem(final AppendableListItem<Integer> item)
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
				item.add(new AjaxLink<Void>("prepend")
				{
					@Override
					public void onClick(AjaxRequestTarget target)
					{
						item.append(counter++);
					}
				})                                    ;
			}
		});
	}

}
