/*
 * Copyright 2018 Gunnar Flötteröd
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package stockholm.ihop2.regent.costwriting;

import java.util.Map;
import java.util.logging.Logger;

import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatrixUtils;

import floetteroed.utilities.math.Histogram;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class HalfTourCostMatrixCalculator implements Runnable {

	private final Matrix halfTourCostMatrix;

	private final String costType;

	private final String actType;

	private final Map<String, Histogram> actType2tourStartTimeHist;

	private final double freqSumTourStart;

	private final Map<String, Histogram> actType2returnTripStartTimeHist;

	private final double freqSumReturnTripStart;

	private final TripCostMatrices tripCostMatrices;

	HalfTourCostMatrixCalculator(final Matrix halfTourCostMatrix,
			final String costType, final String actType,
			final Map<String, Histogram> actType2tourStartTimeHist,
			final double freqSumTourStart,
			final Map<String, Histogram> actType2returnTripStartTimeHist,
			final double freqSumReturnTripStart,
			final TripCostMatrices tripCostMatrices) {
		this.halfTourCostMatrix = halfTourCostMatrix;
		this.costType = costType;
		this.actType = actType;
		this.actType2tourStartTimeHist = actType2tourStartTimeHist;
		this.freqSumTourStart = freqSumTourStart;
		this.actType2returnTripStartTimeHist = actType2returnTripStartTimeHist;
		this.freqSumReturnTripStart = freqSumReturnTripStart;
		this.tripCostMatrices = tripCostMatrices;
	}

	@Override
	public void run() {

		// using own instance for thread safety
		final MatrixUtils matrixUtils = new MatrixUtils();

		Logger.getLogger(this.getClass().getName()).info(
				"Computing " + costType + " for tour purpose " + actType + ".");

		for (int costMatrixBin = 0; costMatrixBin < this.tripCostMatrices
				.getBinCnt(); costMatrixBin++) {
			// contribution of trips towards the activity
			matrixUtils.add(
					halfTourCostMatrix,
					this.tripCostMatrices.getMatrix(costType, costMatrixBin),
					this.actType2tourStartTimeHist.get(actType).freq(
							costMatrixBin + 1)
							/ freqSumTourStart);
			// contribution of trips back from the activity
			matrixUtils.add(
					halfTourCostMatrix,
					this.tripCostMatrices.getMatrix(costType, costMatrixBin),
					this.actType2returnTripStartTimeHist.get(actType).freq(
							costMatrixBin + 1)
							/ freqSumReturnTripStart);
		}

		/*
		 * Dividing tour travel times by half, for compatibility with Regent's
		 * way of processing that data.
		 */

		matrixUtils.mult(halfTourCostMatrix, 0.5);

		/*
		 * Finally, round this down to two digits in order to obtain somewhat
		 * manageable file sizes.
		 */

		matrixUtils.round(halfTourCostMatrix, 2);
	}
}
