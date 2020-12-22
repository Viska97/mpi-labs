package io.github.viska97.mpilabs.lab3

import mpi.Cartcomm
import mpi.MPI
import kotlin.math.sqrt

object MatrixMultiplication {

    private const val ROOT = 0

    private var processCount = 0
    private var rank = 0

    private var matrixSize = 0
    private var blockSize = 0
    private var gridSize = 0
    private lateinit var gridCoordinates: IntArray

    private lateinit var matrixA: IntArray
    private lateinit var matrixB: IntArray
    private lateinit var matrixC: IntArray
    private lateinit var blockA: IntArray
    private lateinit var blockB: IntArray
    private lateinit var blockC: IntArray
    private lateinit var buffer: IntArray

    private lateinit var gridCommunicator: Cartcomm
    private lateinit var columnCommunicator: Cartcomm
    private lateinit var rowCommunicator: Cartcomm

    @JvmStatic
    fun main(args: Array<String>) {
        MPI.Init(args)

        processCount = MPI.COMM_WORLD.Size()
        rank = MPI.COMM_WORLD.Rank()

        val matrixSizeBuffer = IntArray(1)

        if (rank == ROOT) {
            matrixSize = 5
            matrixSizeBuffer[0] = matrixSize
        }

        MPI.COMM_WORLD.Bcast(matrixSizeBuffer, 0, 1, MPI.INT, ROOT)
        matrixSize = matrixSizeBuffer[0]

        gridSize = sqrt(processCount.toDouble()).toInt()

        createGridCommunicators()
        initMatrix()
        sendBlocks()
        multiplyParallel()
        gatherResult()

        if (rank == ROOT) {
            printMatrix("matrix A:", matrixA, matrixSize)
            printMatrix("matrix B:", matrixB, matrixSize)
            printMatrix("matrix C:", matrixC, matrixSize)
        }

        MPI.Finalize()
    }

    private fun createGridCommunicators() {
        val dims = intArrayOf(gridSize, gridSize)
        val periods = booleanArrayOf(false, false)
        gridCommunicator = MPI.COMM_WORLD.Create_cart(dims, periods, false)
        gridCoordinates = gridCommunicator.Coords(rank)
        rowCommunicator = gridCommunicator.Sub(booleanArrayOf(false, true))
        columnCommunicator = gridCommunicator.Sub(booleanArrayOf(true, false))
    }

    private fun initMatrix() {
        blockSize = matrixSize / gridSize

        if (rank == ROOT) {
            matrixA = IntArray(matrixSize * matrixSize) {
                (0..10).random()
            }

            matrixB = IntArray(matrixSize * matrixSize) {
                (0..10).random()
            }

            matrixC = IntArray(matrixSize * matrixSize)
        }

        blockA = IntArray(blockSize * blockSize)
        blockB = IntArray(blockSize * blockSize)
        blockC = IntArray(blockSize * blockSize)
        buffer = IntArray(blockSize * blockSize)
    }

    private fun sendBlocks() {
        if (rank == ROOT) {
            for (r in 0 until processCount) {
                val c = gridCommunicator.Coords(r)

                if (r == ROOT) {
                    blockA = matrixA.copyOfRange(
                        c[0] * matrixSize * blockSize + c[1] * blockSize,
                        c[0] * matrixSize * blockSize + c[1] * blockSize + blockSize * blockSize
                    )
                    blockB = matrixB.copyOfRange(
                        c[0] * matrixSize * blockSize + c[1] * blockSize,
                        c[0] * matrixSize * blockSize + c[1] * blockSize + blockSize * blockSize
                    )
                } else {
                    gridCommunicator.Send(
                        matrixA,
                        c[0] * matrixSize * blockSize + c[1] * blockSize,
                        blockSize * blockSize,
                        MPI.INT,
                        r,
                        0
                    )
                    gridCommunicator.Send(
                        matrixB,
                        c[0] * matrixSize * blockSize + c[1] * blockSize,
                        blockSize * blockSize,
                        MPI.INT,
                        r,
                        0
                    )
                }
            }
        } else {
            gridCommunicator.Recv(blockA, 0, blockSize * blockSize, MPI.INT, ROOT, 0)
            gridCommunicator.Recv(blockB, 0, blockSize * blockSize, MPI.INT, ROOT, 0)
        }
    }

    private fun sendRow(iteration: Int) {
        val p = (gridCoordinates[0] + iteration) % gridSize
        if (gridCoordinates[1] == p) {
            for (i in 0 until blockSize * blockSize) {
                buffer[i] = blockA[i]
            }
        }
        rowCommunicator.Bcast(buffer, 0, blockSize * blockSize, MPI.INT, p)
    }

    private fun multiply() {
        for (i in 0 until blockSize) {
            for (j in 0 until blockSize) {
                for (k in 0 until blockSize) {
                    blockC[i * blockSize + j] += buffer[i * blockSize + k] * blockB[k * blockSize + j]
                }
            }
        }
    }

    private fun sendColumn() {
        var nextProcess = gridCoordinates[0] + 1
        if (gridCoordinates[0] == gridSize - 1) {
            nextProcess = 0
        }
        var prevProcess = gridCoordinates[0] - 1
        if (gridCoordinates[0] == 0) {
            prevProcess = gridSize - 1
        }
        columnCommunicator.Sendrecv_replace(blockB, 0, blockSize * blockSize, MPI.INT, prevProcess, 0, nextProcess, 0)
    }

    private fun multiplyParallel() {
        for (i in 0 until gridSize) {
            sendRow(i)
            multiply()
            sendColumn()
        }
    }

    private fun gatherResult() {
        if (rank != ROOT) {
            gridCommunicator.Send(blockC, 0, blockSize * blockSize, MPI.INT, ROOT, 0)
        } else {
            for (r in 0 until processCount) {
                val c = gridCommunicator.Coords(r)

                if (r == ROOT) {
                    blockC.copyInto(matrixC, c[0] * matrixSize * blockSize + c[1] * blockSize)
                } else {
                    gridCommunicator.Recv(matrixC, c[0] * matrixSize * blockSize + c[1] * blockSize, blockSize * blockSize, MPI.INT, r, 0)
                }
            }
        }
    }

    private fun printMatrix(message: String, matrix: IntArray, Size: Int) {
        val builder = StringBuilder()
        builder.append("$message\n")
        for (i in 0 until Size) {

            for (j in 0 until Size) {
                builder.append(matrix[i * Size + j].toString())
                builder.append(" ")
            }

            builder.append("\n")
        }
        print(builder.toString())
    }
}