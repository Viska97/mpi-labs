package io.github.viska97.mpilabs.lab1

import mpi.MPI

class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MPI.Init(args)
            val me = MPI.COMM_WORLD.Rank()
            val size = MPI.COMM_WORLD.Size()
            println("Hello world from <$me> from <$size>")
            MPI.Finalize()
        }
    }

}