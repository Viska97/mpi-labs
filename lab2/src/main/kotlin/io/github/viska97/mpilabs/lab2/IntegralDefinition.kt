package io.github.viska97.mpilabs.lab2

import mpi.MPI
import kotlin.math.E
import kotlin.math.pow

object IntegralDefinition {

    private const val ROOT = 0

    @JvmStatic
    fun main(args: Array<String>) {
        MPI.Init(args)

        val rank = MPI.COMM_WORLD.Rank()

        val paramsBuffer = if(MPI.COMM_WORLD.Rank() == ROOT) {
            val a = 0.0
            val b = 5.0
            val numProc = (MPI.COMM_WORLD.Size() - 1).toDouble()

            println("process $rank; broadcast params: a: $a, b: $b, numProc: $numProc")

            doubleArrayOf(a, b, numProc)
        }
        else {
            DoubleArray(3)
        }

        MPI.COMM_WORLD.Bcast(paramsBuffer, 0, paramsBuffer.size, MPI.DOUBLE, ROOT)

        val sumBuffer = DoubleArray(1)
        val resultBuffer = DoubleArray(1)

        if(MPI.COMM_WORLD.Rank() != ROOT) {
            sumBuffer[0] = calculateLocalSum(paramsBuffer)
        }

        MPI.COMM_WORLD.Reduce(sumBuffer, 0, resultBuffer, 0, 1, MPI.DOUBLE, MPI.SUM, ROOT)

        MPI.COMM_WORLD.Barrier()

        if(MPI.COMM_WORLD.Rank() == ROOT) {
            val result = resultBuffer[0]

            println("process $rank; result: $result")
        }

        MPI.Finalize()
    }

    private fun calculateLocalSum(paramsBuffer: DoubleArray): Double {
        val a = paramsBuffer[0]
        val b = paramsBuffer[1]
        val numProc = paramsBuffer[2].toInt()

        val rank = MPI.COMM_WORLD.Rank()

        val len = (b - a) / numProc
        val localA = a + (rank-1) * len
        val localB = localA + len

        val localSum = f((localA + localB)/2) * (localB - localA)

        println("process $rank; interval: [$localA:$localB]; local sum: $localSum")

        return localSum
    }

    private fun f(x: Double): Double = E.pow(x)
}