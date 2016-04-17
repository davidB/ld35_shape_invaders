/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld35;

import javax.inject.Inject;
import javax.inject.Singleton;

import jme3_ext.Command;

@Singleton
public class Commands {
	public final Command<Boolean> exit = new Command<>("quit");
	public final Command<Float> changeShape = new Command<>("changeShape");
	public final Command<Float> moveLeft = new Command<>("moveLeft");
	public final Command<Float> moveRight = new Command<>("moveRight");
	public final Command<Boolean> action1 = new Command<>("action1");

	public final Command<?>[] all = {changeShape, moveLeft, moveRight, action1, exit};
	
	@Inject
	public Commands(){}
}
