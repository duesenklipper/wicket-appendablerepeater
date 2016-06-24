package de.wicketbuch.extensions.appendablerepeater.gridview;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import java.util.Map;

import static sun.swing.MenuItemLayoutHelper.max;

public abstract class AppendableGridView<T> extends GridView<T> {

    private Map<Integer, AppendableItem<T>> renderedEmptyItems;
    private long lastItemCount = 0;

    public AppendableGridView(String id, IDataProvider dataProvider) {
        super(id, dataProvider);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getParent().setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (renderedEmptyItems != null) {
            renderedEmptyItems.clear();
        }
    }

    @Override
    protected AppendableItem<T> newEmptyItem(String id, int index) {
        return new AppendableItem<>(id, index);
    }

    @Override
    protected AppendableItem<T> newItem(String id, int index, IModel<T> model) {
        return new AppendableItem<>(id, index, model);
    }

    @Override
    protected AppendableRowItem newRowItem(String id, int index) {
        return new AppendableRowItem(id, index);
    }

    public boolean itemsAppended(AjaxRequestTarget ajax) {
        final long lastPage = getPageCount() - 1;
        if (getCurrentPage() == lastPage) {
            // not on the last page -> just go to last page
            ajax.add(getParent());
            setCurrentPage(lastPage);
            return false;
        } else {
            final long newItemCount = getItemCount();
            final long unrenderedItemCount = newItemCount - lastItemCount;
            if (unrenderedItemCount > 0) {
                // need to render new items
                final long itemsPerPage = getItemsPerPage();
                final long itemsOnLastPage = lastItemCount % itemsPerPage;
                if (itemsOnLastPage < itemsPerPage) {

                }
            } else {

            }
        }
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
        this.lastItemCount = getItemCount();
    }

    /**
     * Created by calle on 24.06.16.
     */
    protected class AppendableItem<T> extends Item<T> {
        public AppendableItem(String id, int index, IModel<T> model) {
            super(id, index, model);
            setOutputMarkupId(true);
        }

        public AppendableItem(String id, int index) {
            this(id, index, null);
        }

        @Override
        protected void onAfterRender() {
            super.onAfterRender();
            AppendableGridView.this.maxRenderedIndex = max(AppendableGridView.this.maxRenderedIndex, this.getIndex());
        }
    }

    protected class AppendableRowItem extends Item<Object> {
        public AppendableRowItem(String id, int index) {
            super(id, index);
        }
    }
}
