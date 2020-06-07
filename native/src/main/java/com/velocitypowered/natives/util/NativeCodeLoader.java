package com.velocitypowered.natives.util;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NativeCodeLoader<T extends @NonNull Object> implements Supplier<T> {

  private final List<Variant<T>> variants;
  private @MonotonicNonNull Variant<T> selected;

  NativeCodeLoader(List<Variant<T>> variants) {
    this.variants = variants;
  }

  @EnsuresNonNull("selected")
  private void selectVariant() {
    if (selected == null) {
      for (Variant<T> variant : variants) {
        @Nullable T got = variant.get();
        if (got != null) {
          this.selected = variant;
          return;
        }
      }
      throw new IllegalArgumentException("Can't find any suitable variants");
    }
  }

  @Override
  public T get() {
    if (selected == null) {
      this.selectVariant();
    }
    assert this.selected.constructed != null : "@AssumeAssertion(nullness): at most one variant"
        + " will be selected and used, and as part of initialization, the object would be"
        + " constructed";
    return selected.constructed;
  }

  /**
   * Gets the name of the native used.
   *
   * @return the name of the native in use
   */
  public String getLoadedVariant() {
    if (selected == null) {
      this.selectVariant();
    }
    return selected.name;
  }

  static class Variant<T> {

    private Status status;
    private final Runnable setup;
    private final String name;
    private final Supplier<T> object;
    private @Nullable T constructed;

    Variant(BooleanSupplier possiblyAvailable, Runnable setup, String name, T object) {
      this(possiblyAvailable, setup, name, () -> object);
    }

    Variant(BooleanSupplier possiblyAvailable, Runnable setup, String name, Supplier<T> object) {
      this.status =
          possiblyAvailable.getAsBoolean() ? Status.POSSIBLY_AVAILABLE : Status.NOT_AVAILABLE;
      this.setup = setup;
      this.name = name;
      this.object = object;
    }

    public @Nullable T get() {
      if (status == Status.NOT_AVAILABLE || status == Status.SETUP_FAILURE) {
        return null;
      }

      // Make sure setup happens only once
      if (status == Status.POSSIBLY_AVAILABLE) {
        try {
          setup.run();
          constructed = object.get();
          if (constructed == null) {
            throw new IllegalStateException("Unable to construct native object.");
          }
          status = Status.SETUP;
        } catch (Exception e) {
          status = Status.SETUP_FAILURE;
          return null;
        }
      }

      return constructed;
    }
  }

  private enum Status {
    NOT_AVAILABLE,
    POSSIBLY_AVAILABLE,
    SETUP,
    SETUP_FAILURE
  }

  static final BooleanSupplier ALWAYS = () -> true;
}
