package com.ternaryop.photoshelf;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.tumblr.Blog;

public class BlogActionProvider extends ActionProvider {
	private Context context;
    private ImageLoader imageLoader;

	public BlogActionProvider(Context context) {
		super(context);
		this.context = context;
		this.imageLoader = new ImageLoader(context, "avatar");
	}

	@Override
	public View onCreateActionView() {
		return null;
	}

	@Override
	public boolean onPerformDefaultAction() {
		return super.onPerformDefaultAction();
	}

	@Override
	public boolean hasSubMenu() {
		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		subMenu.clear();

		int index = 0;
		Drawable drawable = context.getResources().getDrawable(R.drawable.stub);
		for (final String blogName : new AppSupport(context).getBlogList()) {
			// set an initial icon otherwise the icon loaded in displayIcon doesn't appear
			final MenuItem menuItem = subMenu.add(R.id.group_menu_actionbar_blog, index++, Menu.NONE, blogName)
			.setIcon(drawable);
			imageLoader.displayIcon(Blog.getAvatarUrlBySize(blogName, 32), menuItem);
		}
	}
}
