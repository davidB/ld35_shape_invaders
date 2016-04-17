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
			new Wave(5, 2.5f, new int[]{
					-1,-1, 1,-1,-1,
					-1, 0, 1, 0,-1,
					 1, 0, 1, 0, 1,
			}),
			new Wave(5, 3f, new int[]{
					 1,-1, 1,-1, 1,
					 0, 0, 0, 0, 0,
					 2, 0, 2, 0, 2,
			}),
			new Wave(5, 3.5f, new int[]{
					 0, 1, 2, 3, 4,
					 0, 1, 2, 3, 4,
					 0, 1, 2, 3, 4,
					 0, 1, 2, 3, 4,
					 0, 1, 2, 3, 4,
			}),
			new Wave(5, 4f, new int[]{
					-1,-1, 1, 1,-1,
					-1, 2, 2, 2,-1,
					 3, 3, 3, 3, 3,
					 4, 4, 4, 4, 4,
			}),
			new Wave(5, 4f, new int[]{
					 0, 0, 0, 0, 0,
					 0, 4, 4, 4, 0,
					 0, 4, 5, 4, 0,
					 0, 4, 2, 4, 0,
					 0, 5, 5, 5, 0,
					 3, 3, 3, 3, 3,
			}),
			new Wave(5, 3f, new int[]{
					 0, 0, 0, 0, 0,
					 1, 1, 1, 1, 1,
					 2, 2, 2, 2, 2,
					 3, 3, 3, 3, 3,
					 4, 4, 4, 4, 4,
					 5, 5, 5, 5, 5,
					 4, 4, 4, 4, 4,
					 3, 3, 3, 3, 3,
					 2, 2, 2, 2, 2,
					 1, 1, 1, 1, 1,
					 0, 0, 0, 0, 0,
			}),
		};
	
}
