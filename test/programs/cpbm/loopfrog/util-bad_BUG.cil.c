/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 211 "/usr/lib/gcc/x86_64-linux-gnu/4.4.3/include/stddef.h"
typedef unsigned long size_t;
#line 119 "/usr/include/ctype.h"
extern  __attribute__((__nothrow__)) int toupper(int __c ) ;
#line 339 "/usr/include/stdio.h"
extern int printf(char const   * __restrict  __format  , ...) ;
#line 819
extern void perror(char const   *__s ) ;
#line 127 "/usr/include/string.h"
extern  __attribute__((__nothrow__)) char *strcpy(char * __restrict  __dest , char const   * __restrict  __src )  __attribute__((__nonnull__(1,2))) ;
#line 397
extern  __attribute__((__nothrow__)) size_t strlen(char const   *__s )  __attribute__((__pure__,
__nonnull__(1))) ;
#line 471 "/usr/include/stdlib.h"
extern  __attribute__((__nothrow__)) void *malloc(size_t __size )  __attribute__((__malloc__)) ;
#line 72 "sendmail-bad.h"
char *xalloc(int sz ) ;
#line 73
void buildfname(char *gecos , char *login , char *buf ) ;
#line 88 "util-bad.c"
char *xalloc(int sz ) 
{ register char *p ;
  void *tmp ;
  unsigned int __cil_tmp4 ;
  size_t __cil_tmp5 ;
  void *__cil_tmp6 ;
  unsigned long __cil_tmp7 ;
  unsigned long __cil_tmp8 ;

  {
#line 95
  if (sz <= 0) {
#line 96
    sz = 1;
  } else {

  }
  {
#line 98
  __cil_tmp4 = (unsigned int )sz;
#line 98
  __cil_tmp5 = (size_t )__cil_tmp4;
#line 98
  tmp = malloc(__cil_tmp5);
#line 98
  p = (char *)tmp;
  }
  {
#line 99
  __cil_tmp6 = (void *)0;
#line 99
  __cil_tmp7 = (unsigned long )__cil_tmp6;
#line 99
  __cil_tmp8 = (unsigned long )p;
#line 99
  if (__cil_tmp8 == __cil_tmp7) {
    {
#line 101
    perror("Out of memory!!");
    }
  } else {

  }
  }
#line 103
  return (p);
}
}
#line 143 "util-bad.c"
void buildfname(char *gecos , char *login , char *buf ) 
{ register char *p ;
  register char *bp ;
  int l ;
  size_t tmp ;
  size_t tmp___0 ;
  size_t tmp___1 ;
  int tmp___2 ;
  char *tmp___3 ;
  size_t tmp___4 ;
  char __cil_tmp13 ;
  int __cil_tmp14 ;
  char __cil_tmp15 ;
  int __cil_tmp16 ;
  char __cil_tmp17 ;
  int __cil_tmp18 ;
  char __cil_tmp19 ;
  int __cil_tmp20 ;
  char __cil_tmp21 ;
  int __cil_tmp22 ;
  char __cil_tmp23 ;
  int __cil_tmp24 ;
  char const   *__cil_tmp25 ;
  size_t __cil_tmp26 ;
  size_t __cil_tmp27 ;
  char __cil_tmp28 ;
  int __cil_tmp29 ;
  char __cil_tmp30 ;
  int __cil_tmp31 ;
  char __cil_tmp32 ;
  int __cil_tmp33 ;
  char __cil_tmp34 ;
  int __cil_tmp35 ;
  char __cil_tmp36 ;
  int __cil_tmp37 ;
  char const   * __restrict  __cil_tmp38 ;
  char const   *__cil_tmp39 ;
  char const   *__cil_tmp40 ;
  char const   * __restrict  __cil_tmp41 ;
  char * __restrict  __cil_tmp42 ;
  char const   * __restrict  __cil_tmp43 ;
  char __cil_tmp44 ;
  int __cil_tmp45 ;
  char __cil_tmp46 ;
  int __cil_tmp47 ;
  char const   * __restrict  __cil_tmp48 ;
  int __cil_tmp49 ;
  char const   *__cil_tmp50 ;
  char const   * __restrict  __cil_tmp51 ;

  {
#line 150
  bp = buf;
  {
#line 153
  __cil_tmp13 = *gecos;
#line 153
  __cil_tmp14 = (int )__cil_tmp13;
#line 153
  if (__cil_tmp14 == 42) {
#line 154
    gecos = gecos + 1;
  } else {

  }
  }
#line 157
  l = 0;
#line 158
  p = gecos;
  {
#line 158
  while (1) {
    while_continue: /* CIL Label */ ;
    {
#line 158
    __cil_tmp15 = *p;
#line 158
    __cil_tmp16 = (int )__cil_tmp15;
#line 158
    if (__cil_tmp16 != 0) {
      {
#line 158
      __cil_tmp17 = *p;
#line 158
      __cil_tmp18 = (int )__cil_tmp17;
#line 158
      if (__cil_tmp18 != 44) {
        {
#line 158
        __cil_tmp19 = *p;
#line 158
        __cil_tmp20 = (int )__cil_tmp19;
#line 158
        if (__cil_tmp20 != 59) {
          {
#line 158
          __cil_tmp21 = *p;
#line 158
          __cil_tmp22 = (int )__cil_tmp21;
#line 158
          if (__cil_tmp22 != 37) {

          } else {
#line 158
            goto while_break;
          }
          }
        } else {
#line 158
          goto while_break;
        }
        }
      } else {
#line 158
        goto while_break;
      }
      }
    } else {
#line 158
      goto while_break;
    }
    }
    {
#line 160
    __cil_tmp23 = *p;
#line 160
    __cil_tmp24 = (int )__cil_tmp23;
#line 160
    if (__cil_tmp24 == 38) {
      {
#line 161
      __cil_tmp25 = (char const   *)login;
#line 161
      tmp = strlen(__cil_tmp25);
#line 161
      __cil_tmp26 = (size_t )l;
#line 161
      __cil_tmp27 = __cil_tmp26 + tmp;
#line 161
      l = (int )__cil_tmp27;
      }
    } else {
#line 163
      l = l + 1;
    }
    }
#line 158
    p = p + 1;
  }
  while_break: /* CIL Label */ ;
  }
#line 167
  p = gecos;
  {
#line 167
  while (1) {
    while_continue___0: /* CIL Label */ ;
    {
#line 167
    __cil_tmp28 = *p;
#line 167
    __cil_tmp29 = (int )__cil_tmp28;
#line 167
    if (__cil_tmp29 != 0) {
      {
#line 167
      __cil_tmp30 = *p;
#line 167
      __cil_tmp31 = (int )__cil_tmp30;
#line 167
      if (__cil_tmp31 != 44) {
        {
#line 167
        __cil_tmp32 = *p;
#line 167
        __cil_tmp33 = (int )__cil_tmp32;
#line 167
        if (__cil_tmp33 != 59) {
          {
#line 167
          __cil_tmp34 = *p;
#line 167
          __cil_tmp35 = (int )__cil_tmp34;
#line 167
          if (__cil_tmp35 != 37) {

          } else {
#line 167
            goto while_break___0;
          }
          }
        } else {
#line 167
          goto while_break___0;
        }
        }
      } else {
#line 167
        goto while_break___0;
      }
      }
    } else {
#line 167
      goto while_break___0;
    }
    }
    {
#line 169
    __cil_tmp36 = *p;
#line 169
    __cil_tmp37 = (int )__cil_tmp36;
#line 169
    if (__cil_tmp37 == 38) {
      {
#line 172
      __cil_tmp38 = (char const   * __restrict  )"strcpy(bp,login)\n";
#line 172
      printf(__cil_tmp38);
#line 173
      __cil_tmp39 = (char const   *)login;
#line 173
      tmp___0 = strlen(__cil_tmp39);
#line 173
      __cil_tmp40 = (char const   *)bp;
#line 173
      tmp___1 = strlen(__cil_tmp40);
#line 173
      __cil_tmp41 = (char const   * __restrict  )"strlen(bp) = %d strlen(login) = %d\n";
#line 173
      printf(__cil_tmp41, tmp___1, tmp___0);
      }
      {
#line 177
      __cil_tmp42 = (char * __restrict  )bp;
#line 177
      __cil_tmp43 = (char const   * __restrict  )login;
#line 177
      strcpy(__cil_tmp42, __cil_tmp43);
#line 178
      __cil_tmp44 = *bp;
#line 178
      __cil_tmp45 = (int )__cil_tmp44;
#line 178
      tmp___2 = toupper(__cil_tmp45);
#line 178
      *bp = (char )tmp___2;
      }
      {
#line 179
      while (1) {
        while_continue___1: /* CIL Label */ ;
        {
#line 179
        __cil_tmp46 = *bp;
#line 179
        __cil_tmp47 = (int )__cil_tmp46;
#line 179
        if (__cil_tmp47 != 0) {

        } else {
#line 179
          goto while_break___1;
        }
        }
#line 180
        bp = bp + 1;
      }
      while_break___1: /* CIL Label */ ;
      }
    } else {
      {
#line 185
      tmp___3 = bp;
#line 185
      bp = bp + 1;
#line 185
      *tmp___3 = *p;
#line 186
      __cil_tmp48 = (char const   * __restrict  )"bp-buf = %d\n";
#line 186
      __cil_tmp49 = bp - buf;
#line 186
      printf(__cil_tmp48, __cil_tmp49);
      }
    }
    }
#line 167
    p = p + 1;
  }
  while_break___0: /* CIL Label */ ;
  }
  {
#line 189
  *bp = (char )'\000';
#line 191
  __cil_tmp50 = (char const   *)buf;
#line 191
  tmp___4 = strlen(__cil_tmp50);
#line 191
  __cil_tmp51 = (char const   * __restrict  )"buf can store at most %d bytes; strlen(buf) = %d\n";
#line 191
  printf(__cil_tmp51, 5, tmp___4);
  }
#line 192
  return;
}
}
