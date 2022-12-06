package in.myinnos.awesomeimagepicker.helpers;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class EndlessRecyclerGridOnScrollListener extends RecyclerView.OnScrollListener {

    private int previousTotal = 0; // The total number of items in the dataset after the last load
    private boolean loading = true; // True if we are still waiting for the last set of data to load.
    private int visibleThreshold = 10; // The minimum amount of items to have below your current scroll position before loading more.
    int firstVisibleItem, visibleItemCount, totalItemCount;

    private int currentPage = 1;

    private GridLayoutManager gridLayoutManager;

    public EndlessRecyclerGridOnScrollListener(GridLayoutManager gridLayoutManager) {
        this.gridLayoutManager = gridLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        visibleItemCount = recyclerView.getChildCount();
        totalItemCount = gridLayoutManager.getItemCount();
        firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition();

        if (loading) {
            if (totalItemCount > previousTotal+1) {
                loading = false;
                previousTotal = totalItemCount;
            }
        }
        boolean shouldLoadMore = false;
        if ((gridLayoutManager).getStackFromEnd()) {
            // Calculate differently if this is a reverse scroll, ex: a chat application where
            // newest message is at the bottom
            shouldLoadMore = firstVisibleItem == visibleThreshold;
        } else {
            shouldLoadMore = (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold);
        }
        if (!loading && shouldLoadMore) {
            // End has been reached
            // Do something
            currentPage++;
            onLoadMore(currentPage);
            loading = true;
        }
    }

    public abstract void onLoadMore(int currentPage);

    public void resetPages() {
        currentPage = 1;
        previousTotal = 0;
        loading = true;
    }

    public int getFirstVisibleItem() {
        return gridLayoutManager.findFirstVisibleItemPosition();
    }
}
