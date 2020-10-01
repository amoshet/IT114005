//Ahmed Moshet
//Java Loops HW

public class JavaLoops {
	public static void main(String[] args) {
		int[] x = { 5, 12, 35, 70, 300 };

		for (int i = 0; i < x.length; i++) {
			// if the remainder of x[i]/2 is 0, it is even and is printed.
			if (x[i] % 2 == 0) {
				System.out.println(x[i]);
			}
		}
	}
}
