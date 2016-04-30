package de.wicketbuch.extensions.appendablerepeater.examples;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

/**
 * Created by calle on 19/04/16.
 */
public class AppendableRepeaterApplication extends WebApplication
{
	@Override
	public Class<? extends Page> getHomePage()
	{
		return HomePage.class;
	}
}
