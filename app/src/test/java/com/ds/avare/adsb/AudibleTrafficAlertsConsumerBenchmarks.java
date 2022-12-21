package com.ds.avare.adsb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.options.*;
import org.openjdk.jmh.runner.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class AudibleTrafficAlertsConsumerBenchmarks {

    private static final AudibleTrafficAlerts ata = getTestAudibleTrafficAlerts(10);

    public static void main(String[] args) throws Exception {
        runBenchmarks(AudibleTrafficAlertsConsumerBenchmarks.class.getName(), "*");
    }

    @State(Scope.Thread)
    public static class SoundListState {
        public final ArrayList<Integer> soundIds = new ArrayList<>(16);
        @TearDown(Level.Iteration)
        public void doTearDown() {
            soundIds.clear();
        }
    }

    @Benchmark @BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void numberSequenceBuild(SoundListState state)  {
        ata.numberFormatOption = AudibleTrafficAlerts.NumberFormatOption.INDIVIDUAL_DIGIT;
        ata.distanceCalloutOption = AudibleTrafficAlerts.DistanceCalloutOption.DECIMAL;
        ata.addNumericalAlertAudio(state.soundIds, 1049.99, true);
    }

    @Benchmark @BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void colloquialBuild(SoundListState state)  {
        ata.numberFormatOption = AudibleTrafficAlerts.NumberFormatOption.COLLOQUIAL;
        ata.distanceCalloutOption = AudibleTrafficAlerts.DistanceCalloutOption.DECIMAL;
        ata.addNumericalAlertAudio(state.soundIds, 1049.99, true);
    }

    @Test
    public void testNumberSequenceBenchmark() throws Exception {
        final RunResult runResult = runBenchmarks("numberSequenceBuild");
        final Result primResult = runResult.getPrimaryResult();
        Assert.assertTrue("Score for "+primResult.getLabel()+" not high enough: "+primResult.getScore(),
                primResult.getScore() > 2000);
        final Result gcAllocResult = runResult.getSecondaryResults().get("·gc.alloc.rate");
        Assert.assertTrue("GC alloc rate too high: "+gcAllocResult.getScore(),
                gcAllocResult.getScore() < 1000);
    }

    @Test
    public void testColloquialBenchmark() throws Exception {
        final RunResult runResult = runBenchmarks("numberSequenceBuild");
        final Result primResult = runResult.getPrimaryResult();
        Assert.assertTrue("Score for "+primResult.getLabel()+" not high enough: "+primResult.getScore(),
                primResult.getScore() > 2000);
        final Result gcAllocResult = runResult.getSecondaryResults().get("·gc.alloc.rate");
        Assert.assertTrue("GC alloc rate too high: "+gcAllocResult.getScore(),
                gcAllocResult.getScore() < 1000);
    }

    private static RunResult runBenchmarks(String classname, String methodName)  throws RunnerException {
        final Options opt = new OptionsBuilder()
                .include(classname+"."+methodName)
                .addProfiler("stack", "lines=7;top=10;detailLine=true;period=5;excludePackages=true")
                .addProfiler("gc", "churn=true")
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(1)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(1)
                .threads(2)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        return new Runner(opt).run().iterator().next();
    }

    private RunResult runBenchmarks(String benchmarkName) throws RunnerException {
        return runBenchmarks(this.getClass().getName(), benchmarkName);
    }

    private static AudibleTrafficAlerts getTestAudibleTrafficAlerts(int secondsCount) {
        final int[] seconds = new int[secondsCount];
        for (int i = 0; i < secondsCount; i++)
            seconds[i] = 2000 + i;
        return new AudibleTrafficAlerts(getMockSoundPlayer(), mock(Context.class));
    }

    private static AudibleTrafficAlerts.SequentialSoundPoolPlayer getMockSoundPlayer() {
        AudibleTrafficAlerts.SequentialSoundPoolPlayer sp = mock(AudibleTrafficAlerts.SequentialSoundPoolPlayer.class);
        when(sp.load(any(), any()))
                .thenAnswer(invocation -> {
                    int[] regurg = new int[invocation.getArguments().length-1];
                    for (int i = 1; i < invocation.getArguments().length; i++)
                        regurg[i-1] = (int) invocation.getArgument(i);
                    return regurg;
                });
        return sp;
    }
}