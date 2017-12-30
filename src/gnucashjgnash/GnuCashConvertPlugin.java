/*
 * Copyright 2017 Albert Santos.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package gnucashjgnash;

import gnucashjgnash.action.ImportGnuCashAction;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import jgnash.plugin.FxPlugin;
import jgnash.plugin.SwingPlugin;
import jgnash.uifx.views.main.MainView;

public class GnuCashConvertPlugin implements SwingPlugin, FxPlugin {
    private static final int MENU_INDEX = 2;

    @Override
    public String getName() {
        return "GnuCash To jGnash";
    }

    @Override
    public void start(PluginPlatform pluginPlatform) {
        if (pluginPlatform == PluginPlatform.Fx) {
            installFxMenu();
        }
    }

    private void installFxMenu() {
        final MenuBar menuBar = MainView.getInstance().getMenuBar();

        menuBar.getMenus().stream().filter(menu -> "fileMenu".equals(menu.getId())).forEach(menu -> menu.getItems()
                .stream().filter(menuItem -> menuItem instanceof Menu)
                .filter(menuItem -> "importMenu".equals(menuItem.getId())).forEach(menuItem -> {

                    final MenuItem importMenuItem = new MenuItem(GnuCashConvertUtil.getString("Menu.GnuCashTojGnash.Name"));

                    importMenuItem.setOnAction(event -> ImportGnuCashAction.showAndWait(MainView.getPrimaryStage()));

                    ((Menu) menuItem).getItems().add(importMenuItem);
                }));
    }
}
