package auth

import (
	"context"
	"errors"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func withGate(t *testing.T, concurrent, waiting int, wait time.Duration) {
	t.Helper()
	ConfigureArgon2(concurrent, waiting, wait)
	t.Cleanup(func() { ConfigureArgon2(DefaultArgon2MaxConcurrent, DefaultArgon2MaxWaiting, DefaultArgon2MaxWait) })
}

func TestArgon2GateRefusesWhenSaturatedAndNobodyMayWait(t *testing.T) {
	withGate(t, 1, 0, time.Second)
	release, err := ReserveArgon2Slot(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	defer release()

	start := time.Now()
	_, err = HashPasswordContext(context.Background(), "pw", make([]byte, saltSize))
	if !errors.Is(err, ErrBusy) {
		t.Fatalf("err = %v, want ErrBusy", err)
	}
	if time.Since(start) > 500*time.Millisecond {
		t.Fatal("a refused request must fail fast instead of waiting")
	}
}

func TestArgon2GateWaiterTimesOut(t *testing.T) {
	withGate(t, 1, 4, 60*time.Millisecond)
	release, err := ReserveArgon2Slot(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	defer release()
	start := time.Now()
	_, err = ReserveArgon2Slot(context.Background())
	if !errors.Is(err, ErrBusy) {
		t.Fatalf("err = %v, want ErrBusy", err)
	}
	if d := time.Since(start); d < 50*time.Millisecond || d > 2*time.Second {
		t.Fatalf("waited %v, want about 60ms", d)
	}
}

func TestArgon2GateWaiterIsServedWhenASlotFrees(t *testing.T) {
	withGate(t, 1, 4, 5*time.Second)
	release, _ := ReserveArgon2Slot(context.Background())
	done := make(chan error, 1)
	go func() {
		r, err := ReserveArgon2Slot(context.Background())
		if err == nil {
			r()
		}
		done <- err
	}()
	time.Sleep(50 * time.Millisecond)
	release()
	select {
	case err := <-done:
		if err != nil {
			t.Fatalf("waiter failed: %v", err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("waiter was not served after a slot was released")
	}
}

func TestArgon2GateHonoursContextCancellation(t *testing.T) {
	withGate(t, 1, 4, 5*time.Second)
	release, _ := ReserveArgon2Slot(context.Background())
	defer release()
	ctx, cancel := context.WithCancel(context.Background())
	go func() { time.Sleep(30 * time.Millisecond); cancel() }()
	_, err := ReserveArgon2Slot(ctx)
	if !errors.Is(err, context.Canceled) {
		t.Fatalf("err = %v, want context.Canceled", err)
	}
	if !IsCapacityError(err) || !IsCapacityError(ErrBusy) || IsCapacityError(errors.New("x")) {
		t.Fatal("IsCapacityError classification is wrong")
	}
}

func TestArgon2NeverRunsMoreThanTheConfiguredNumberAtOnce(t *testing.T) {
	withGate(t, 2, 64, 10*time.Second)
	var active, peak atomic.Int64
	var wg sync.WaitGroup
	for i := 0; i < 12; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			release, err := gate.Load().acquire(context.Background())
			if err != nil {
				t.Error(err)
				return
			}
			n := active.Add(1)
			for {
				p := peak.Load()
				if n <= p || peak.CompareAndSwap(p, n) {
					break
				}
			}
			time.Sleep(15 * time.Millisecond)
			active.Add(-1)
			release()
		}()
	}
	wg.Wait()
	if peak.Load() > 2 {
		t.Fatalf("peak concurrency %d exceeds the limit of 2", peak.Load())
	}
	if peak.Load() < 2 {
		t.Fatalf("peak concurrency %d: the gate should still allow 2 at once", peak.Load())
	}
}

func TestArgon2StillHashesAndVerifies(t *testing.T) {
	salt, err := GenerateSalt()
	if err != nil {
		t.Fatal(err)
	}
	h, err := HashPasswordContext(context.Background(), "secret", salt)
	if err != nil {
		t.Fatal(err)
	}
	ok, err := VerifyPasswordContext(context.Background(), "secret", base64Raw(salt), h)
	if err != nil || !ok {
		t.Fatalf("verify = %v, %v", ok, err)
	}
	ok, err = VerifyPasswordContext(context.Background(), "other", base64Raw(salt), h)
	if err != nil || ok {
		t.Fatalf("wrong password verify = %v, %v", ok, err)
	}
	// Malformed stored data is a data error, not a capacity error.
	if _, err := VerifyPasswordContext(context.Background(), "x", "!!!", h); err == nil || IsCapacityError(err) {
		t.Fatalf("malformed salt: err = %v", err)
	}
}
