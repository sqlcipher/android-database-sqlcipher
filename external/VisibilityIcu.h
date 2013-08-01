/* Dirty hack changing ICU visibility to hidden */

#ifndef _VISIBILITY_ICU_H
#define _VISIBILITY_ICU_H

#include <unicode/platform.h>
#undef U_EXPORT
#define U_EXPORT

#endif
