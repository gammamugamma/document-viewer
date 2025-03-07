package org.ebookdroid.core;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.models.DocumentModel.PageIterator;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HScrollController extends AbstractScrollController {

    public HScrollController(final IActivityController base) {
        super(base, DocumentViewMode.HORIZONTAL_SCROLL);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState, final int firstVisible, final int lastVisible) {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final int viewX = Math.round(viewState.viewRect.centerX());

        final PageIterator pages = firstVisible != -1 ? viewState.model.getPages(firstVisible, lastVisible + 1)
                : viewState.model.getPages(0);
        try {
            final RectF bounds = new RectF();
            for (final Page page : pages) {
                viewState.getBounds(page, bounds);
                final int pageX = Math.round(bounds.centerX());
                final long dist = Math.abs(pageX - viewX);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    result = page.index.viewIndex;
                }
            }
        } finally {
            pages.release();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        final AppSettings app = AppSettings.current();
        final int dx = (int) (direction * getWidth() * (app.scrollHeight / 100.0));

        if (app.animateScrolling) {
            getView().startPageScroll(dx, 0);
        } else {
            getView().scrollBy(dx, 0);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#verticalConfigAutoScroll()
     */
    @Override
    public final void verticalConfigAutoScroll() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#isAutoScrolling()
     */
    @Override
    public final boolean isAutoScrolling() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#stopAutoScrolling()
     */
    @Override
    public final void stopAutoScrolling() {
    }

    private boolean isRightToLeft() {
        final BookSettings bs = base.getBookSettings();
        return bs.rtl;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getScrollLimits()
     */
    @Override
    public final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final Page lpo = isRightToLeft() ? model.getPageObject(0) : model.getLastPageObject();

        final float zoom = getBase().getZoomModel().getZoom();

        final int right = lpo != null ? (int) lpo.getBounds(zoom).right - width : 0;
        final int bottom = (int) (height * zoom) - height;

        return new Rect(0, 0, right, bottom);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#invalidatePageSizes(org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public synchronized final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized) {
            return;
        }

        if (reason == InvalidateSizeReason.PAGE_ALIGN) {
            return;
        }

        final int height = getHeight();
        final int width = getWidth();
        final BookSettings bookSettings = base.getBookSettings();
        final PageAlign pageAlign = DocumentViewMode.getPageAlign(bookSettings);

        if (changedPage == null || isRightToLeft()) {
            float widthAccum = 0;

            List<Page> pages = new ArrayList<Page>(Arrays.asList(model.getPages()));
            if (isRightToLeft()) {
                Collections.reverse(pages);
            }

            for (final Page page : pages) {
                final RectF pageBounds = calcPageBounds(pageAlign, page.getAspectRatio(), width, height);
                pageBounds.offset(widthAccum, 0);
                page.setBounds(pageBounds);
                widthAccum += pageBounds.width() + 3;
            }
        } else {
            // TODO: Implement this for the isRightToLeft() case
            float widthAccum = changedPage.getBounds(1.0f).left;
            List<Page> pages = model.getPageList(changedPage.index.viewIndex, model.getPageCount());

            for (final Page page : pages) {
                final RectF pageBounds = calcPageBounds(pageAlign, page.getAspectRatio(), width, height);
                pageBounds.offset(widthAccum, 0);
                page.setBounds(pageBounds);
                widthAccum += pageBounds.width() + 3;
            }
        }
    }

    @Override
    public RectF calcPageBounds(final PageAlign pageAlign, final float pageAspectRatio, final int width,
            final int height) {
        return new RectF(0, 0, height * pageAspectRatio, height);
    }
}
