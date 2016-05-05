package mesosphere.marathon.plugin.plugin

/**
  * Opt functions generally perform some mutable operation on some arg of
  * type T and possibly return an "undo" Opt implementation.
  */
trait Opt[T] extends Function1[T, Option[Opt[T]]]

object Opt {
  /**
    * OptFactory functions consume some parameter P and generate a functional
    * configuration option for type T.
    */
  trait Factory[P, T] extends Function1[P, Option[Opt[T]]]

  object Factory {
    def noop[P, T]: Factory[P, T] = new Factory[P, T] {
      override def apply(p: P): Option[Opt[T]] = Some(Opt.noop)
    }
  }

  // TODO(jdef) we probably don't need the dsl stuff at all, this is just me playing with ideas.
  // I generally dislike dsl's.
  object dsl {
    import scala.language.implicitConversions

    /** invoke allows for tuple notation to apply an Opt to some T: `t -> someOpt` */
    implicit def applyOpt[T](t: Tuple2[T, Opt[T]]): Option[Opt[T]] = t._2(t._1)
    /**
      * invoke allows for tuple notation to apply a config parametger to some T to
      * generate a functional option: `p -> t` (for example `secretsLabelFactory -> appDef`).
      */
    implicit def applyParam[P, T](t: Tuple2[Factory[P, T], P]): Option[Opt[T]] = t._1(t._2)
  }

  /**
    * applyAll invokes each provided Opt, in order, on the given `t`
    * @return the result of the last invoked Opt, or else a no-op Opt
    */
  def applyAll[T](t: T, opts: Opt[T]*): Option[Opt[T]] = {
    var last: Option[Opt[T]] = Some(noop[T])
    for (o <- opts) {
      last = o(t)
    }
    last
  }

  /**
    * noop always returns an Opt that makes NO changes to its T argument
    * @return an Opt that does nothing, and always returns Some Opt that does nothing
    */
  def noop[T]: Opt[T] = new Opt[T] {
    override def apply(t: T): Option[Opt[T]] = Some(noop)
  }

  def combine[T](opts: Opt[T]*): Opt[T] = new Opt[T] {
    override def apply(t: T): Option[Opt[T]] = {
      applyAll(t, opts: _*)
    }
  }
}
