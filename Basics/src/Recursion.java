//Ahmed Moshet
//Recursion HW

public class Recursion {
	public static int sum(int num) {
		if (num > 0) {
			return num + sum(num - 1);
		}
		return 0;
	}

	public static void main(String[] args) {
		System.out.println(sum(10));
	}
}
