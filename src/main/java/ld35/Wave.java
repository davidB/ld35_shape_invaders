package ld35;

public class Wave {
	public final int width;
	public final float baseSpeed;
	public final int[] layout;

	public Wave(int width, float baseSpeed, int[] layout) {
	super();
		this.width = width;
		this.baseSpeed = baseSpeed;
		this.layout = layout;
	}

	public static final Wave[] waves = {
			new Wave(5, 2f, new int[]{
				-1,-1, 0,-1,-1,
				-1, 0, 0, 0,-1,
				 0, 0, 0, 0, 0,
			}),
	};
	
}
