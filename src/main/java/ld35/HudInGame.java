/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld35;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class HudInGame {
//	@FXML
//	public Region root;

	@FXML
	public Label text;

	@FXML
	public HBox textGroup;

	@Inject
	public HudInGame() {
	}
}
