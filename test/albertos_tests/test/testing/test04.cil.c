/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is true */

#line 1 "test04.c"
int foo(int x , int n ) 
{ int i ;
  int y ;

  {
#line 3
  y = 1;
#line 5
  i = 0;
#line 5
  while (1) {
#line 5
    if (i < n) {

    } else {
#line 5
      break;
    }
#line 6
    y *= x;
#line 5
    i += 1;
  }
#line 9
  return (y);
}
}
