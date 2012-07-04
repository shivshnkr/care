package nz.ac.massey.cs.care.refactoring.movehelper;

import java.util.Arrays;
/**
 * Selection criteria for choosing the move refactoring direction:
 * i) 0 represents no refactoring can be performed
 * ii) 12 represent move the source class to the target class's package
 * iii) 21 repreent move the target class to the source class's package
 * @author alishah
 *
 */
public class Criteria {
	
	public static int choose(int instances, int instAfter1,
			int refacReq1, int instAfter2, int refacReq2) {
		int nums[] = { instAfter1, instAfter2 };
		Arrays.sort(nums);
		if(instances <= nums[0]) return 0;
		else if(instAfter1 < instAfter2) return 12;
		else if(instAfter2 < instAfter1) return 21;
		else {
			if(instAfter1 == instAfter2 && refacReq1 <= refacReq2) return 12;
			else return 21;
		}
	}
	
	public static int choose1(int instances, int instAfter1, int instAfter2) {
		int nums[] = { instAfter1, instAfter2 };
		Arrays.sort(nums);
		if(instances <= nums[0]) return 0;
		else if(instAfter1 < instAfter2) return 12;
		else return 21;
		
	}
}
