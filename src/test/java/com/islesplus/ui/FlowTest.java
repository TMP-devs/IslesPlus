package com.islesplus.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlowTest {
    static class Box extends Widget { final int pw, ph; Box(int pw, int ph){this.pw=pw;this.ph=ph;}
        public int layout(int x,int y,int width){this.x=x;this.y=y;this.w=Math.min(pw==Integer.MAX_VALUE?width:pw,width);this.h=ph;return ph;}
        public int prefWidth(){return pw;} public void render(net.minecraft.client.gui.DrawContext c,int a,int b){} }
    @Test void columnStacksWithGap() { var c=new Flow.Column(5).add(new Box(10,10)).add(new Box(10,20)); assertEquals(35,c.layout(0,0,100)); assertEquals(15,c.children().get(1).y); }
    @Test void hiddenChildrenTakeNoSpace() { var b=new Box(10,10); b.visible=false; assertEquals(10,new Flow.Column(5).add(b).add(new Box(10,10)).layout(0,0,100)); }
    @Test void rowWraps() { var a=new Box(60,10); var b=new Box(60,12); var r=new Flow.WrapRow(4,3).add(a).add(b); assertEquals(25,r.layout(0,0,100)); assertEquals(0,b.x); assertEquals(13,b.y); }
    @Test void fillChildTakesRemainder() { var a=new Box(30,10); var f=new Box(Integer.MAX_VALUE,10); new Flow.WrapRow(4,3).add(a).add(f).layout(0,0,200); assertEquals(34,f.x); assertEquals(166,f.w); }
    @Test void fillChildWrapsWhenCramped() { var a=new Box(80,10); var f=new Box(Integer.MAX_VALUE,10); var r=new Flow.WrapRow(4,3).add(a).add(f); assertEquals(23,r.layout(0,0,100)); assertEquals(100,f.w); }

    @Test void threeFillChildrenShareOneLineEqually() {
        var a = new Box(Integer.MAX_VALUE, 10);
        var b = new Box(Integer.MAX_VALUE, 10);
        var c = new Box(Integer.MAX_VALUE, 10);
        new Flow.WrapRow(4, 3).add(a).add(b).add(c).layout(0, 0, 194);
        assertEquals(62, a.w); assertEquals(0, a.x);
        assertEquals(62, b.w); assertEquals(66, b.x);
        assertEquals(62, c.w); assertEquals(132, c.x);
    }

    @Test void fixedFillFixedLaysOutOnOneLineWithFillGettingTheRemainder() {
        var label = new Box(28, 10);
        var slider = new Box(Integer.MAX_VALUE, 10);
        var value = new Box(30, 10);
        new Flow.WrapRow(4, 3).add(label).add(slider).add(value).layout(0, 0, 194);
        assertEquals(0, label.x);
        assertEquals(32, slider.x);
        assertEquals(128, slider.w); // 194 - 28 - 30 - 8 (two gaps)
        assertEquals(164, value.x);
    }

    @Test void secondOfTwoFillChildrenWrapsWhenCramped() {
        var a = new Box(Integer.MAX_VALUE, 10);
        var b = new Box(Integer.MAX_VALUE, 10);
        var r = new Flow.WrapRow(4, 3).add(a).add(b);
        r.layout(0, 0, 100); // 60 + 4 + 60 = 124 > 100: b wraps to its own line
        assertEquals(100, a.w);
        assertEquals(100, b.w);
    }

    @Test void fillFixedFillSharesLeftoverEquallyAroundAFixedChild() {
        var a = new Box(Integer.MAX_VALUE, 10);
        var mid = new Box(80, 10);
        var c = new Box(Integer.MAX_VALUE, 10);
        new Flow.WrapRow(4, 3).add(a).add(mid).add(c).layout(0, 0, 300);
        assertEquals(106, a.w);
        assertEquals(80, mid.w);
        assertEquals(106, c.w); // 300 - 80 - 8 (two gaps) = 212, split 106/106
    }

    @Test void gridLaysOutTwoColumnsWithGapAndTallestRowHeight() {
        var a = new Box(Integer.MAX_VALUE, 18);
        var b = new Box(Integer.MAX_VALUE, 24);
        var c = new Box(Integer.MAX_VALUE, 18);
        var g = new Flow.Grid(2, 4, 150).add(a).add(b).add(c);
        assertEquals(46, g.layout(0, 0, 200)); // row1 = tallest(18,24) = 24, gap 4, row2 = 18
        assertEquals(98, a.w);                 // (200 - 4) / 2
        assertEquals(0, a.x);
        assertEquals(102, b.x);                // 98 + gap 4
        assertEquals(0, b.y);
        assertEquals(0, c.x);                  // wrapped onto the second row
        assertEquals(28, c.y);                 // 24 + gap 4
    }

    @Test void gridCollapsesToOneColumnBelowThreshold() {
        var a = new Box(Integer.MAX_VALUE, 18);
        var b = new Box(Integer.MAX_VALUE, 18);
        var g = new Flow.Grid(2, 4, 150).add(a).add(b);
        assertEquals(40, g.layout(0, 0, 149)); // 18 + gap 4 + 18
        assertEquals(149, a.w);
        assertEquals(0, b.x);
        assertEquals(22, b.y);
    }

    @Test void gridSkipsHiddenChildrenAndHasNoTrailingGap() {
        var a = new Box(Integer.MAX_VALUE, 18);
        var hidden = new Box(Integer.MAX_VALUE, 18); hidden.visible = false;
        var b = new Box(Integer.MAX_VALUE, 18);
        var g = new Flow.Grid(2, 4, 150).add(a).add(hidden).add(b);
        assertEquals(18, g.layout(0, 0, 200)); // both visible tiles share one row, no trailing gap
        assertEquals(102, b.x);
        assertEquals(0, b.y);
    }

    @Test void gridForwardsClicksLikeAnyOtherContainer() {
        var sibling = new Recorder();
        var consumer = new Recorder(); consumer.clickReturn = true;
        var g = new Flow.Grid(1, 4, 0).add(sibling).add(consumer);
        g.layout(0, 0, 100); // sibling y=0..10, consumer y=14..24
        assertTrue(g.mouseClicked(5, 16, 0));
        assertFalse(consumer.unfocusCalled);
        assertTrue(sibling.unfocusCalled);
    }

    /** Records which input methods were invoked on it, and lets a test control what it returns. */
    static class Recorder extends Widget {
        boolean clickedCalled, releasedCalled, scrolledCalled, unfocusCalled;
        boolean clickReturn, scrollReturn;
        public int layout(int x, int y, int width) { this.x = x; this.y = y; this.w = width; this.h = 10; return 10; }
        public void render(net.minecraft.client.gui.DrawContext c, int a, int b) {}
        @Override public boolean mouseClicked(double mx, double my, int button) { clickedCalled = true; return clickReturn; }
        @Override public void mouseReleased() { releasedCalled = true; }
        @Override public boolean mouseScrolled(double mx, double my, double amount) { scrolledCalled = true; return scrollReturn; }
        @Override public void unfocus() { unfocusCalled = true; }
    }

    @Test void hiddenChildSkippedInReverseOrderClickForwardingStopsAtFirstTrue() {
        var earliest = new Recorder(); earliest.clickReturn = true;
        var a = new Recorder(); a.clickReturn = true;
        var hidden = new Recorder(); hidden.visible = false; hidden.clickReturn = true;
        var c = new Recorder(); c.clickReturn = false;
        var col = new Flow.Column(0).add(earliest).add(a).add(hidden).add(c);
        assertTrue(col.mouseClicked(1, 1, 0));
        assertTrue(c.clickedCalled);       // checked first (reverse order), returns false
        assertFalse(hidden.clickedCalled); // hidden: never receives the call
        assertTrue(a.clickedCalled);       // checked next, returns true -> stop
        assertFalse(earliest.clickedCalled); // forwarding stopped before reaching it
    }

    @Test void consumedClickUnfocusesEveryOtherChildButNotTheConsumer() {
        var sibling1 = new Recorder();
        var consumer = new Recorder(); consumer.clickReturn = true;
        var sibling2 = new Recorder();
        var col = new Flow.Column(0).add(sibling1).add(consumer).add(sibling2);
        col.layout(0, 0, 100); // sibling1 y=0..10, consumer y=10..20, sibling2 y=20..30
        assertTrue(col.mouseClicked(5, 15, 0)); // inside consumer's bounds
        assertFalse(consumer.unfocusCalled);
        assertTrue(sibling1.unfocusCalled);
        assertTrue(sibling2.unfocusCalled);
    }

    @Test void consumedClickDeepInNestedContainerUnfocusesSiblingContainersChildren() {
        var deepConsumer = new Recorder(); deepConsumer.clickReturn = true;
        var containerA = new Flow.Column(0).add(deepConsumer);
        var siblingInB = new Recorder();
        var containerB = new Flow.Column(0).add(siblingInB);
        var outer = new Flow.Column(0).add(containerA).add(containerB);
        outer.layout(0, 0, 100); // containerA y=0..10, containerB y=10..20
        assertTrue(outer.mouseClicked(5, 5, 0)); // inside containerA/deepConsumer bounds
        assertFalse(deepConsumer.unfocusCalled);
        assertTrue(siblingInB.unfocusCalled);
    }

    @Test void nonConsumingClickUnfocusesChildrenOutsideThePointButNotTheOneUnderIt() {
        var underPoint = new Recorder();
        var elsewhere = new Recorder();
        var col = new Flow.Column(0).add(underPoint).add(elsewhere);
        col.layout(0, 0, 100); // underPoint y=0..10, elsewhere y=10..20
        assertFalse(col.mouseClicked(5, 5, 0)); // inside underPoint's bounds; nothing consumes
        assertFalse(underPoint.unfocusCalled);
        assertTrue(elsewhere.unfocusCalled);
    }

    /** Logs "commit" the first time unfocus() is called and never again, idempotent, like a
     * well-behaved field (e.g. HexField). */
    static class LoggingField extends Widget {
        private final java.util.List<String> log;
        private boolean committed = false;
        LoggingField(java.util.List<String> log) { this.log = log; }
        public int layout(int x, int y, int width) { this.x = x; this.y = y; this.w = width; this.h = 10; return 10; }
        public void render(net.minecraft.client.gui.DrawContext c, int a, int b) {}
        @Override public void unfocus() {
            if (!committed) { committed = true; log.add("commit"); }
        }
    }

    /** Logs "apply" and consumes every click it receives. */
    static class LoggingRail extends Widget {
        private final java.util.List<String> log;
        LoggingRail(java.util.List<String> log) { this.log = log; }
        public int layout(int x, int y, int width) { this.x = x; this.y = y; this.w = width; this.h = 10; return 10; }
        public void render(net.minecraft.client.gui.DrawContext c, int a, int b) {}
        @Override public boolean mouseClicked(double mx, double my, int button) {
            log.add("apply");
            return true;
        }
    }

    @Test void clickAppliedToOneChildCommitsAFocusedSiblingFirst() {
        var log = new java.util.ArrayList<String>();
        var field = new LoggingField(log);
        var rail = new LoggingRail(log);
        var col = new Flow.Column(0).add(field).add(rail);
        col.layout(0, 0, 100); // field y=0..10, rail y=10..20
        assertTrue(col.mouseClicked(5, 15, 0)); // inside rail's bounds
        assertEquals(java.util.List.of("commit", "apply"), log);
    }

    @Test void clickAppliedDeepInSiblingContainerCommitsOtherContainersFieldFirst() {
        var log = new java.util.ArrayList<String>();
        var field = new LoggingField(log);
        var containerA = new Flow.Column(0).add(field);
        var rail = new LoggingRail(log);
        var containerB = new Flow.Column(0).add(rail);
        var outer = new Flow.Column(0).add(containerA).add(containerB);
        outer.layout(0, 0, 100); // containerA y=0..10, containerB y=10..20
        assertTrue(outer.mouseClicked(5, 15, 0)); // inside containerB/rail's bounds
        assertEquals(java.util.List.of("commit", "apply"), log);
    }

    @Test void columnMaintainsOwnBoundsAfterLayoutForContainsToWork() {
        var col = new Flow.Column(5).add(new Box(10, 10)).add(new Box(10, 20));
        assertEquals(35, col.layout(3, 4, 50));
        assertEquals(3, col.x);
        assertEquals(4, col.y);
        assertEquals(50, col.w);
        assertEquals(35, col.h);
        assertTrue(col.contains(3, 4));
        assertTrue(col.contains(52, 38));
        assertFalse(col.contains(53, 4));
        assertFalse(col.contains(3, 39));
    }

    @Test void wrapRowMaintainsOwnBoundsAfterLayoutForContainsToWork() {
        var row = new Flow.WrapRow(4, 3).add(new Box(30, 10));
        assertEquals(10, row.layout(2, 6, 80));
        assertEquals(2, row.x);
        assertEquals(6, row.y);
        assertEquals(80, row.w);
        assertEquals(10, row.h);
    }

    @Test void mouseReleasedAndUnfocusReachHiddenChildren() {
        var hidden = new Recorder(); hidden.visible = false;
        var visible = new Recorder();
        var col = new Flow.Column(0).add(hidden).add(visible);
        col.mouseReleased();
        col.unfocus();
        assertTrue(hidden.releasedCalled);
        assertTrue(visible.releasedCalled);
        assertTrue(hidden.unfocusCalled);
        assertTrue(visible.unfocusCalled);
    }

    @Test void mouseScrolledForwardedInReverseOrderStoppingAtFirstTrue() {
        var a = new Recorder(); a.scrollReturn = true;
        var b = new Recorder(); b.scrollReturn = true;
        var col = new Flow.Column(0).add(a).add(b);
        col.layout(0, 0, 100); // a y=0..10, b y=10..20
        assertTrue(col.mouseScrolled(5, 15, 1)); // inside b
        assertTrue(b.scrolledCalled);   // checked first (reverse order), returns true -> stop
        assertFalse(a.scrolledCalled);  // never reached
    }

    @Test void mouseScrolledNotForwardedToAChildTheCursorIsNotOver() {
        var a = new Recorder(); a.scrollReturn = true;
        var b = new Recorder(); b.scrollReturn = true;
        var col = new Flow.Column(0).add(a).add(b);
        col.layout(0, 0, 100); // a y=0..10, b y=10..20
        assertFalse(col.mouseScrolled(5, 40, 1)); // below both children: nobody is scrolled
        assertFalse(a.scrolledCalled);
        assertFalse(b.scrolledCalled);
    }

    @Test void mouseScrolledSkipsTheChildBesideThePointAndReachesTheOneUnderIt() {
        var beside = new Recorder(); beside.scrollReturn = true;
        var underPoint = new Recorder(); underPoint.scrollReturn = true;
        var col = new Flow.Column(0).add(underPoint).add(beside);
        col.layout(0, 0, 100); // underPoint y=0..10, beside y=10..20
        assertTrue(col.mouseScrolled(5, 5, 1)); // inside underPoint
        assertTrue(underPoint.scrolledCalled);
        assertFalse(beside.scrolledCalled);     // contains() gate skipped it despite reverse order
    }

    // --- non-left clicks: forwarded, but with neither unfocus sweep (so a listening KeyChip
    // can bind a mouse button, and a right click never commits a focused field) ---

    @Test void nonLeftClickIsForwardedToChildrenButTriggersNoUnfocusPrePass() {
        var elsewhere = new Recorder();          // does NOT contain the point
        var consumer = new Recorder(); consumer.clickReturn = true;
        var col = new Flow.Column(0).add(elsewhere).add(consumer);
        col.layout(0, 0, 100); // elsewhere y=0..10, consumer y=10..20
        assertTrue(col.mouseClicked(5, 15, 1)); // right click inside consumer's bounds
        assertTrue(consumer.clickedCalled);      // still forwarded
        assertFalse(elsewhere.unfocusCalled);    // pre-pass did not run
        assertFalse(consumer.unfocusCalled);     // post-consume sweep did not run either
    }

    @Test void nonLeftClickDoesNotUnfocusSiblingsWhenNothingConsumesIt() {
        var a = new Recorder();
        var b = new Recorder();
        var col = new Flow.Column(0).add(a).add(b);
        col.layout(0, 0, 100);
        assertFalse(col.mouseClicked(5, 5, 1));
        assertFalse(a.unfocusCalled);
        assertFalse(b.unfocusCalled);
    }

    @Test void nonLeftClickDoesNotCommitAFocusedFieldBesideTheConsumer() {
        var log = new java.util.ArrayList<String>();
        var field = new LoggingField(log);
        var rail = new LoggingRail(log);
        var col = new Flow.Column(0).add(field).add(rail);
        col.layout(0, 0, 100); // field y=0..10, rail y=10..20
        assertTrue(col.mouseClicked(5, 15, 1)); // right click inside rail's bounds
        assertEquals(java.util.List.of("apply"), log); // no "commit": the field kept its draft
    }

    @Test void leftClickStillRunsBothUnfocusSweeps() {
        var elsewhere = new Recorder();
        var consumer = new Recorder(); consumer.clickReturn = true;
        var col = new Flow.Column(0).add(elsewhere).add(consumer);
        col.layout(0, 0, 100);
        assertTrue(col.mouseClicked(5, 15, 0));
        assertTrue(elsewhere.unfocusCalled);
        assertFalse(consumer.unfocusCalled);
    }

    @Test void verticalCentringOffsetsShorterChildInLine() {
        var a = new Box(30, 10);
        var b = new Box(30, 20);
        new Flow.WrapRow(4, 3).add(a).add(b).layout(0, 0, 100);
        assertEquals(5, a.y);  // shorter child: (lineH 20 - h 10) / 2
        assertEquals(0, b.y);  // tallest child in the line: no offset
    }

    @Test void alignShiftsNonFillLineByLeftoverWidth() {
        var centered = new Box(30, 10);
        new Flow.WrapRow(4, 3).add(centered).align(Flow.Align.CENTER).layout(0, 0, 100);
        assertEquals(35, centered.x); // leftover 70 / 2

        var ended = new Box(30, 10);
        new Flow.WrapRow(4, 3).add(ended).align(Flow.Align.END).layout(0, 0, 100);
        assertEquals(70, ended.x); // leftover 70
    }

    @Test void alignHasNoEffectOnLineWithFillChild() {
        var a = new Box(30, 10);
        var f = new Box(Integer.MAX_VALUE, 10);
        new Flow.WrapRow(4, 3).add(a).add(f).align(Flow.Align.CENTER).layout(0, 0, 200);
        assertEquals(0, a.x);   // same as START: align ignored on a fill line
        assertEquals(34, f.x);
        assertEquals(166, f.w);
    }
}
