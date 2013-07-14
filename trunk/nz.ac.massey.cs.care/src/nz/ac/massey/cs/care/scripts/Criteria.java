package nz.ac.massey.cs.care.scripts;

import java.util.Arrays;

import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
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
	
	/**
	 * Selection criteria for choosing the move refactoring direction:
	 * i) 0 represents no refactoring can be performed
	 * ii) 12 represent move the source class to the target class's package
	 * iii) 21 repreent move the target class to the source class's package
	 * @author alishah
	 *
	 */
	public static int choose1(int instances, int instAfter1, int instAfter2) {
		int nums[] = { instAfter1, instAfter2 };
		Arrays.sort(nums);
		if(instances < nums[0]) return 0;//removed the equal condition
		else if(instAfter1 < instAfter2) return 12;
		else return 21;
		
	}

	public static Candidate choose(Candidate c1,
			Candidate c2) {
		if(c1.getInstancesAfter() <= c2.getInstancesAfter()) {
			c1.setReservedMoveCandidate(c2);
			return c1;
		} else {
			c2.setReservedMoveCandidate(c1);
			return c2;
		}
	}
	public static void choose(Candidate c1, Candidate c2,
			Candidate finalCandidate, Candidate reserveCandidate) {
		if(c1.getInstancesAfter() <= c2.getInstancesAfter()) {
			finalCandidate = c1;
			reserveCandidate = c2;
		} else {
			finalCandidate = c2;
			reserveCandidate = c1;
		}
	}
}
