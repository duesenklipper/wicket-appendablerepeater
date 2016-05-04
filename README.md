# Appendable Repeaters for Wicket

Current versions:
 - **1.1.wicket7** for Wicket 7.x
 - **1.1.wicket6** for Wicket 6.x
 - **1.1.wicket5** for Wicket 1.5.x
 - **1.1.wicket4** for Wicket 1.4.x

Repeaters in Wicket, like `ListView`, are obviously useful. Attaching to the tags that are to be repeated (like `<li>`) rather than the tags that surround them (like `<ul>`) makes them very flexible. The only problem is that this makes it a little awkward to append additional items on the fly in Ajax calls.

Wicket's standard way of doing Ajax updates is by sending markup to the client, where an existing element is swapped out for the new content. When we want to append to a repeater, there is no element to swap out, so this doesn't work, at least not directly.

In [an article on wicketinaction.com](http://wicketinaction.com/2008/10/repainting-only-newly-created-repeater-items-via-ajax/), Igor Vaynberg long ago showed a solution that is fairly simple and works nicely. Before the markup for a new list item, he sends a JavaScript call that creates a new element in the DOM at the appropriate position. This is then used by Wicket as the target for the new markup.

I wanted a small, simple and reusable implementation of this, so I wrote this small library. So far, it contains `AppendableListView`, a replacement for `ListView`. This has an append method that will create a new `ListItem` and then render it after the last element via Ajax. It supports appending multiple items in one Ajax request and provides an `onAppendItem` method to allow animating the new elements. See the appendablerepeater-examples project on how to do that.

It does not (yet) support inserting new elements at arbitrary positions.

To use it, add the following repository to your `pom.xml`:

    <repositories>
      <repository>
        <id>duesenklipper</id>
        <url>http://duesenklipper.github.com/maven/releases</url>
        <snapshots>
          <enabled>false</enabled>
        </snapshots>
        <releases>
          <enabled>true</enabled>
        </releases>
      </repository>
    </repositories>

Then add the following dependency:

    <dependency>
        <groupId>de.wicketbuch.extensions</groupId>
        <artifactId>appendablerepeater</artifactId>
        <version>1.0</version>
    </dependency>

