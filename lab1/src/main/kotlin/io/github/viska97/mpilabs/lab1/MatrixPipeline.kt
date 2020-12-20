package io.github.viska97.mpilabs.lab1

import mpi.MPI

class MatrixPipeline {

    companion object {

        private const val MATRIX_SIZE = 3

        private const val INPUT_SEGMENT = 0
        private const val MINORS_SEGMENT = 1
        private const val INVERSE_SEGMENT = 2

        private const val TAG = 0

        @JvmStatic
        fun main(args: Array<String>) {
            MPI.Init(args)
            when (MPI.COMM_WORLD.Rank()) {
                INPUT_SEGMENT -> inputMatrix()
                MINORS_SEGMENT -> calculateMinors()
                INVERSE_SEGMENT -> inverseMatrix()
            }
            MPI.Finalize()
        }

        private fun inputMatrix() {
            val matrix = arrayOf(
                doubleArrayOf(2.0, 5.0, 7.0),
                doubleArrayOf(6.0, 3.0, 4.0),
                doubleArrayOf(5.0, -2.0, -3.0)
            )

            printMatrix("segment $INPUT_SEGMENT send matrix to segments $MINORS_SEGMENT and $INVERSE_SEGMENT:", matrix)

            MPI.COMM_WORLD.Send(matrix, 0, MATRIX_SIZE, MPI.OBJECT, MINORS_SEGMENT, TAG)
            MPI.COMM_WORLD.Send(matrix, 0, MATRIX_SIZE, MPI.OBJECT, INVERSE_SEGMENT, TAG)
        }

        private fun calculateMinors() {
            val matrix = Array(MATRIX_SIZE) { DoubleArray(MATRIX_SIZE) }
            MPI.COMM_WORLD.Recv(matrix, 0, MATRIX_SIZE, MPI.OBJECT, 0, TAG)

            printMatrix("segment $MINORS_SEGMENT recv matrix from segment $INPUT_SEGMENT:", matrix)

            val minorsMatrix = Array(MATRIX_SIZE) { i ->
                DoubleArray(MATRIX_SIZE) { j ->
                    val minor = DoubleArray(4)
                    var count = 0

                    for (m in 0 until MATRIX_SIZE) {
                        for (n in 0 until MATRIX_SIZE) {
                            if (m != i && n != j) {
                                minor[count++] = matrix[m][n]
                            }
                        }
                    }

                    minor[0] * minor[3] - minor[1] * minor[2]
                }
            }

            printMatrix("segment $MINORS_SEGMENT send minors matrix to segment $INVERSE_SEGMENT", minorsMatrix)

            MPI.COMM_WORLD.Send(minorsMatrix, 0, MATRIX_SIZE, MPI.OBJECT, INVERSE_SEGMENT, TAG)
        }

        fun inverseMatrix() {
            val matrix = Array(MATRIX_SIZE) { DoubleArray(MATRIX_SIZE) }
            val minorsMatrix = Array(MATRIX_SIZE) { DoubleArray(MATRIX_SIZE) }

            MPI.COMM_WORLD.Recv(matrix, 0, MATRIX_SIZE, MPI.OBJECT, INPUT_SEGMENT, TAG)
            MPI.COMM_WORLD.Recv(minorsMatrix, 0, MATRIX_SIZE, MPI.OBJECT, MINORS_SEGMENT, TAG)

            printMatrix("segment $INVERSE_SEGMENT recv matrix from segment $INPUT_SEGMENT:", matrix)
            printMatrix("segment $INVERSE_SEGMENT recv minors matrix from segment $MINORS_SEGMENT:", minorsMatrix)

            val complementMatrix = Array(MATRIX_SIZE) { i ->
                DoubleArray(MATRIX_SIZE) { j ->
                    if ((i == 1 && j != 1) || (i != 1 && j == 1)) {
                        minorsMatrix[i][j] * -1
                    } else {
                        minorsMatrix[i][j]
                    }
                }
            }

            val transposedMatrix = Array(MATRIX_SIZE) { i ->
                DoubleArray(MATRIX_SIZE) { j ->
                    complementMatrix[j][i]
                }
            }

            val determinant = matrix[0][0] * matrix[1][1] * matrix[2][2] +
                    matrix[0][1] * matrix[1][2] * matrix[2][0] +
                    matrix[0][2] * matrix[1][0] * matrix[2][1] -
                    matrix[0][2] * matrix[1][1] * matrix[2][0] -
                    matrix[0][0] * matrix[1][2] * matrix[2][1] -
                    matrix[0][1] * matrix[1][0] * matrix[2][2]

            println("segment $INVERSE_SEGMENT determinant: $determinant")

            val inverseMatrix = (1.0 / determinant) * transposedMatrix

            printMatrix("segment $INVERSE_SEGMENT inverse matrix:", inverseMatrix)
        }

        private fun printMatrix(message: String, matrix: Array<DoubleArray>) {
            println("$message\n${matrix.joinToString(separator = "\n") { it.joinToString(",") }}")
            println()
        }

        operator fun Double.times(matrix: Array<DoubleArray>): Array<DoubleArray> = Array(matrix.size) { i ->
            DoubleArray(matrix[i].size) { j ->
                matrix[i][j] * this
            }
        }
    }

}