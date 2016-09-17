package pmserver;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import pmutils.PMPacket;

class PMClientd
{
  SocketChannel channel;
  PMPacket packet;
  ByteBuffer buffer;
  boolean validated;
  cState cstate;
  String command;
  ArrayList<ByteBuffer> cmdArgs;
  ByteBuffer args;
  boolean waitBinary;
  int binLength;
  PMProcState pState;

  enum cState
  {
    CS_START, CS_WAIT_EXEC_RESULTS, CS_WAIT_STATUS_RESULTS, CS_FIN
  }

  // PMClient states
  PMClientd(SocketChannel chan, PMPacket pkt, ByteBuffer buf) {
    channel = chan;
    packet = pkt;
    buffer = buf;
    validated = false;
    cstate = cState.CS_START;
    waitBinary = false;
    binLength = -1;
  }
} // class PMClientd