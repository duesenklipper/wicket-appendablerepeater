**Current version**: 1.5.0.wicket{7|8} for Wicket 7.x, 8.x

**New:** `AppendableListView` can now remove items.

# Appendable Repeaters for Wicket

Repeaters in Wicket, like `ListView`, are obviously useful. Attaching to the 
tags that are to be repeated (like `<li>`) rather than the tags that surround
them (like `<ul>`) makes them very flexible. The only problem is that this makes
it a little awkward to append additional items on the fly in Ajax calls.

Wicket's standard way of doing Ajax updates is by sending markup to the client,
where an existing element is swapped out for the new content. When we want to
append to a repeater, there is no element to swap out, so this doesn't work, at
least not directly.

In [an article on wicketinaction.com](http://wicketinaction.com/2008/10/repainting-only-newly-created-repeater-items-via-ajax/),
Igor Vaynberg long ago showed a solution that is fairly simple and works nicely.
Before the markup for a new list item, he sends a JavaScript call that creates a
new element in the DOM at the appropriate position. This is then used by Wicket
as the target for the new markup.

I wanted a small, simple and reusable implementation of this, so I wrote this
small library. So far, it contains:

* `AppendableListView`, a replacement for `ListView`.

  This has an `append` method that will create a new `ListItem` and then 
  render it after the last element via Ajax. It supports appending multiple 
  items in one Ajax request and provides an `onAppendItem` method to allow 
  animating the new elements. See the `appendablerepeater-examples` project 
  on how to do that.
  
  It does not (yet) support inserting new elements at arbitrary positions.

* `AppendableGridView`, a replacement for `GridView`.

  Here you add data to the `DataProvider` manually, then trigger 
  `AppendableGridView.itemsAppended()`. It will then figure out what new 
  elements are there and where to put them. Like `AppendableListView`, it 
  gives you callbacks to animate the transition, see the JavaDoc and the 
  examples for more.
  
  It only supports adding items at the end, not arbitrary insertion.
  
## Maven coordinates

    <dependency>
        <groupId>de.wicketbuch.extensions</groupId>
        <artifactId>appendablerepeater</artifactId>
        <version>1.5.0.wicket7</version>
    </dependency>

Make sure you choose the correct version for the version of Wicket you are
using, they are suffixed with `.wicket4`, `.wicket5`, `.wicket6`, `.wicket7` 
respectively.

This project uses [Semantic Versioning](http://semver.org/), so you can rely on
things not breaking within a major version.
