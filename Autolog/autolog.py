"""Easy automatic logging of pretty much anything.

Usage:
 * By decorating a function with @autolog(), it will
   automatically log function name, arguments, and return value to the
   default logger when the function returns
 * By decorating a class with @autolog(), all of its methods
   are automatically decorated
 * Logs with the built-in logging module
 * By decorating a class with @autolog_class() and setting the keyword arguments
   log_private / log_properties to True, calls to its private methods / its property accesses and sets
   will be logged as well
 * Demo at the bottom of this file :)

Disclaimer: This code is just an idea I had and does not intend to be
the most efficient way of accomplishing its goal, only a very interesting way.

Other cool stuff in here:
 * @check_disable decorator: decorates a decorator definition so
   that when a condition is met (in this case when the logging
   level is below the configured disable logging level) the
   decorator being decorated has no effect when used (this is great
   for tacking @autolog onto a bunch of things so that when
   debugging, the disable logging level can be lowered so logging is
   done as expected, but when in production, the disable logging
   level can be left at the default and @autolog will have no effect
   on the code)
 * @default() decorator: because Python evaluates argument defaults
   once (at the function definition), I use this decorator to allow
   for argument defaults to be evaluated at every call (such as when
   I want to check the config object for the latest config)
 * @autolog() and @autolog_XXX() decorators: (bonus: the default
   behavior is for this to have no effect because of @check_disable,
   but this can be changed through @autolog(level=XXX),
   config.DISABLE_LEVEL, or config.DEFAULT_LEVEL)

.. moduleauthor:: Elan Ronen <elanronen@gmail.com>
"""

from functools import wraps
from inspect import isclass
from types import SimpleNamespace
import logging
from typing import Callable, Type, Any, TypeVar

_Func = TypeVar("_Func", bound=Callable[..., Any])
_Class = TypeVar("_Class", bound=Type)
_Logger = logging.getLoggerClass()

# Set parameters below to configure the autologger
config = SimpleNamespace(
    DEFAULT_LEVEL=logging.DEBUG,
    DEFAULT_LOGGER=logging.getLogger(__name__),
    DISABLE_LEVEL=logging.WARNING
)


def _return_default_msg() -> Callable[..., str]:
    def default_msg(func, ret, *args, **kwargs):
        # EXAMPLE: '"arg1", 2, True'
        args_str = ', '.join(map(repr, args))
        # EXAMPLE: 'arg1=0, hi="college person"'
        kwargs_str = ', '.join(f'{k}={repr(v)}' for k, v in kwargs.items())
        # EXAMPLE: <function foo at 0x696969>(2, 'jelly', sec='beans', sep='\t') -> 42
        return f"{func}({args_str}{', ' if args_str and kwargs_str else ''}{kwargs_str}) -> {repr(ret)}"

    return default_msg


def _return_default_level() -> int:
    return config.DEFAULT_LEVEL


def _return_default_logger() -> _Logger:
    return config.DEFAULT_LOGGER


def check_disable(old_decorator_factory):
    """
    Disables the decorator from having an effect if its logging level is at or bellow `config.DISABLE_LEVEL` (place
    this decorator closest to the target function)
    :param old_decorator_factory: The decorator factory to disable.
    :return: The modified decorator factory
    """

    @wraps(old_decorator_factory)
    def disable_checked_decorator_factory(*args, **kwargs):
        # If a level kwarg is given and it should be disabled
        if isinstance(level := kwargs.get("level"), int) and level <= config.DISABLE_LEVEL:
            # Return a decorator that doesn't wrap the function (makes the target decorator have no effect)
            return lambda func: func
        else:
            # Return what the decorator factory would have returned (makes this decorator have no effect)
            return old_decorator_factory(*args, **kwargs)

    return disable_checked_decorator_factory


def default(**default_kwargs: Callable[[], Any]) -> Callable[[_Func], _Func]:
    """
    Decorator that gives a function default values that are evaluated at every call
    :param default_kwargs: every argument of the target function in order set to a method that returns a default value
    :return: the decorator to wrap the target function
    """

    # store the keys of the default values for fast indexing
    default_keys = tuple(default_kwargs.keys())

    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            return func(
                *args,
                **kwargs,
                # fill in args and kwargs not given
                **{
                    # arg/kwarg: generated arg/kwarg default value
                    k: default_kwargs[k]()
                    # start at args not given; end at the rest of the args/kwargs
                    for i in range(len(args), len(default_keys))
                    # if the arg/kwarg was not given as a kwarg, include it
                    if (k := default_keys[i]) not in kwargs
                }
            )

        return wrapper

    return decorator


@default(msg=_return_default_msg, level=_return_default_level, logger=_return_default_logger)
@check_disable
def autolog(msg: Callable[..., str], *, level: int, logger: _Logger) -> Callable[[Any], Any]:
    """
    Decorator analogous to decorating a class or function  with `@autolog_class` or `@autolog_func`
    :param msg: a function that is fed context to generate log text for each use (see `autolog_*`)
    :param level: the logging level to use (defaults to DEBUG) (see the `logging` module's `Logger.log`)
    :param logger: the `Logger` to use (defaults to getLogger(__name__)) (see the `logging` module's `Logger`)
    :return: the appropriate decorator
    """

    def decorator(obj):
        # choose the correct decorator
        wrapper = autolog_func if callable(obj) and not isclass(obj) else autolog_class
        # apply the decorator to the object with the parameters
        return wrapper(msg, level=level, logger=logger)(obj)

    return decorator


@default(
    msg=_return_default_msg, level=_return_default_level, logger=_return_default_logger,
    log_private=bool, log_properties=bool
)
@check_disable
def autolog_class(
        msg: Callable[..., str], *, level: int, logger: _Logger, log_private: bool, log_properties: bool
) -> Callable[[_Class], _Class]:
    """
    Decorator that applies `@autolog_func` to every method of every object created by the class.
    :param msg: `msg(function, return_value, *args_of_func, **kwargs_of_func)`
    (defaults to `f"{func}(*args_of_func, **kwargs_of_func) -> {return_value}"`) (see `autolog`)
    :param level: (see `autolog`)
    :param logger: (see `autolog`)
    :param log_private: whether to apply `@autolog_func` to methods whose names begin with '_' (but don't end with '__')
    :param log_properties: whether to log when non-callable members are accessed or set
    :return: the class decorator
    """

    def decorator(cls):
        if log_properties:
            _log_class_properties(cls, msg, level, logger, log_private)
        for member_name in \
                filter(lambda name: not name.endswith("__"), dir(cls)) \
                if log_private else \
                filter(lambda name: not name.startswith("_"), dir(cls)):
            member = getattr(cls, member_name)
            if not isclass(member) and callable(member):
                setattr(cls, member_name, autolog_func(msg, level=level, logger=logger)(member))

        return cls

    return decorator


def _log_class_properties(cls: _Class, msg: Callable[..., str], level: int, logger: _Logger, log_private: bool) -> None:
    old_getattribute = cls.__getattribute__
    old_setattribute = cls.__setattr__

    @wraps(old_getattribute)
    def logged_getattribute(self, name):
        ret = old_getattribute(self, name)
        if (log_private or not name.startswith("_")) and not callable(ret):
            logger.log(level, msg(old_getattribute, ret, name))
        return ret

    @wraps (old_setattribute)
    def logged_setattribute(self, name, value):
        old_setattribute(self, name, value)
        if log_private or not name.startswith("_"):
            logger.log(level, msg(old_setattribute, None, name, value))

    setattr(cls, "__getattribute__", logged_getattribute)
    setattr(cls, "__setattr__", logged_setattribute)


@default(msg=_return_default_msg, level=_return_default_level, logger=_return_default_logger)
@check_disable
def autolog_func(msg: Callable[..., str], *, level: int, logger: _Logger) -> Callable[[_Func], _Func]:
    """
    Decorator that logs after the target method returns
    :param msg: `msg(function, return_value, *args_of_func, **kwargs_of_func)`
    (defaults to `f"{func}(*args_of_func, **kwargs_of_func) -> {return_value}"`) (see `autolog`)
    :param level: (see `autolog`)
    :param logger: (see `autolog`)
    :return: the function decorator
    """

    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            ret = func(*args, **kwargs)
            logger.log(level, msg(func, ret, *args, **kwargs))
            return ret

        return wrapper

    return decorator


if __name__ == "__main__":
    print("~~~ Testing autolog! ~~~")

    from sys import stdout

    logging.basicConfig(stream=stdout, encoding="utf-8", level=logging.NOTSET)

    config.DISABLE_LEVEL = logging.INFO


    @autolog()
    def boo1(word):
        return f"{word}? This shouldn't have printed :("


    @autolog(level=logging.WARNING)
    def boo2(word):
        return f"{word}! This should print :)"


    new_logger = logging.getLogger("newlogger")


    @autolog(lambda *args, **kwargs: "CUSTOM MESSAGE", level=logging.CRITICAL, logger=new_logger)
    def extra_moo():
        return "This string won't print :O"


    @autolog_func()
    def foo1(word):
        return f"{word}? This shouldn't have printed :("


    @autolog_func(level=logging.WARNING)
    def foo2(word):
        return f"{word}! This should print :)"


    # Test logging disabling
    print("\nShould NOT print: default logging.DEBUG is less severe than disabled logging level (logging.INFO)")
    boo1("BOO1")
    # Test logging disabling and default args/kwargs
    print("\nSHOULD print: logging.WARNING is more severe than disabled logging level (logging.INFO)")
    boo2("BOO2")
    # Test when args/kwargs are provided
    print("\nSHOULD print 'CUSTOM MESSAGE' from 'newlogger': see decorator factory arguments")
    extra_moo()
    # Test disabling on function-specific autolog decorator
    print("\nShould NOT print: default logging.DEBUG is less severe than disabled logging level (logging.INFO)")
    foo1("FOO1")
    # Test disabling on function-specific autolog decorator
    print("\nSHOULD print: logging.WARNING is more severe than disabled logging level (logging.INFO)")
    foo2("FOO2")
