package com.antigravity.peqconfigurator.protocol

/** Spec §26: operation-level error classes surfaced to the user. */
sealed class DacException(message: String) : Exception(message) {
    class UnsupportedDevice(msg: String) : DacException(msg)
    class UnsupportedProtocol(msg: String) : DacException(msg)
    class PermissionDenied(msg: String) : DacException(msg)
    class ConnectionFailed(msg: String) : DacException(msg)
    class ReadFailed(msg: String) : DacException(msg)
    class WriteFailed(msg: String) : DacException(msg)
    class VerificationFailed(msg: String) : DacException(msg)
    class InvalidParameter(msg: String) : DacException(msg)
    class UnsupportedParameter(msg: String) : DacException(msg)
    class DeviceDisconnected(msg: String) : DacException(msg)
    class UnknownDeviceState(msg: String) : DacException(msg)
}
