package intercoach.exception;

public class ExecutionCapacityExceededException extends RuntimeException {

    public ExecutionCapacityExceededException() {
        super("Code execution is at capacity. Please try again shortly.");
    }
}
