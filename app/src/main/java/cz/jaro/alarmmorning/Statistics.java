package cz.jaro.alarmmorning;

import java.util.Arrays;

public class Statistics {

    static public double mean(double[] nums) {
        double total = 0;
        for (double value : nums) {
            total += value;
        }
        return total / nums.length;
    }

    static public double variance(double[] nums) {
        if (nums.length <= 1) {
            return 0;
        }

        double mean = mean(nums);

        double sum = 0;
        for (double value : nums) {
            sum += Math.pow(value - mean, 2);
        }

        return sum / nums.length;
    }

    static public double stdDev(double[] nums) {
        return Math.sqrt(variance(nums));
    }

    /**
     * Note: the side effect is that the array is sorted.
     *
     * @param nums Samples.
     * @return Median of the samples.
     */
    static public double median(double[] nums) {
        Arrays.sort(nums);

        if (nums.length % 2 == 0) {
            return (nums[nums.length / 2] + nums[nums.length / 2 - 1]) / 2;
        } else {
            return nums[nums.length / 2];
        }
    }
}