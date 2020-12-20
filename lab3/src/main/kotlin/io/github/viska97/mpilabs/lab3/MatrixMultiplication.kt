package io.github.viska97.mpilabs.lab3

import mpi.Cartcomm
import mpi.Datatype
import mpi.MPI
import kotlin.math.sqrt

object MatrixMultiplication {

    @JvmStatic
    fun main(args: Array<String>) {
        MPI.Init(args)

        MPI.Finalize()
    }

    private fun printMatrix(message: String, matrix: Array<DoubleArray>) {
        println("$message\n${matrix.joinToString(separator = "\n") { it.joinToString(",") }}")
        println()
    }
}